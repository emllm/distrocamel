@Grab('javax.mail:javax.mail-api:1.6.2')
@Grab('com.sun.mail:javax.mail:1.6.2')
@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')

import javax.mail.*
import javax.mail.internet.*
import javax.activation.*
import groovy.json.JsonSlurper
import java.nio.file.*
import java.util.Properties

// Configuration from environment variables
def imapHost = System.getenv('IMAP_HOST') ?: 'localhost'
def imapPort = System.getenv('IMAP_PORT') ?: '1143'
def imapUser = System.getenv('IMAP_USER') ?: 'client@example.com'
def imapPass = System.getenv('IMAP_PASSWORD') ?: 'password'
def deploymentDir = System.getenv('DEPLOYMENT_DIR') ?: '/app/deployments'

// Create deployment directory if it doesn't exist
new File(deploymentDir).mkdirs()

// Email properties
def props = new Properties()
props.put('mail.store.protocol', 'imap')
props.put('mail.imap.host', imapHost)
props.put('mail.imap.port', imapPort)
props.put('mail.imap.ssl.enable', 'false')
props.put('mail.imap.starttls.enable', 'false')

def session = Session.getDefaultInstance(props, null)

def processAttachments(Part p, File outputDir) {
    def content = p.getContent()
    
    if (content instanceof Multipart) {
        // Process each part
        (0..<content.count).each { i ->
            def bodyPart = content.getBodyPart(i)
            processAttachments(bodyPart, outputDir)
        }
    } else if (p.getDisposition() != null && p.getDisposition().equalsIgnoreCase(Part.ATTACHMENT) || 
               p.getDisposition() == null && p.getFileName() != null) {
        // Save attachment
        def fileName = p.getFileName()
        def file = new File(outputDir, fileName)
        
        // Write the file
        file.withOutputStream { out ->
            out << p.getInputStream()
        }
        
        // Make executable if it's a shell script
        if (fileName.endsWith('.sh')) {
            file.setExecutable(true, false)
        }
        
        println "Saved attachment: ${file.absolutePath}"
        
        // If it's a zip file, extract it
        if (fileName.endsWith('.zip')) {
            def zipFile = new java.util.zip.ZipFile(file)
            zipFile.entries().each { entry ->
                def entryFile = new File(outputDir, entry.name)
                if (entry.directory) {
                    entryFile.mkdirs()
                } else {
                    entryFile.parentFile.mkdirs()
                    entryFile.withOutputStream { out ->
                        out << zipFile.getInputStream(entry)
                    }
                    if (entry.name.endsWith('.sh')) {
                        entryFile.setExecutable(true, false)
                    }
                }
            }
            zipFile.close()
            println "Extracted zip file: ${file.absolutePath}"
        }
    }
}

def checkEmails() {
    try {
        def store = session.getStore('imap')
        store.connect(imapUser, imapPass)
        
        def inbox = store.getFolder('INBOX')
        inbox.open(Folder.READ_WRITE)
        
        // Get all unread messages
        def messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false))
        
        if (messages.length > 0) {
            println "Found ${messages.length} new message(s)"
            
            messages.each { msg ->
                try {
                    // Create a unique directory for this deployment
                    def timestamp = new Date().format('yyyyMMdd_HHmmss')
                    def appDir = new File("${deploymentDir}/app_${timestamp}")
                    appDir.mkdirs()
                    
                    println "Processing message from: ${msg.from[0]}"
                    println "Subject: ${msg.subject}"
                    
                    // Process the message content and attachments
                    processAttachments(msg, appDir)
                    
                    // Look for a run.sh or similar and execute it
                    def runScript = new File(appDir, 'run.sh')
                    if (runScript.exists()) {
                        println "Executing run script: ${runScript.absolutePath}"
                        def process = [runScript.absolutePath].execute(null, appDir)
                        process.consumeProcessOutput(System.out, System.err)
                        process.waitFor()
                    } else {
                        // Look for other common entry points
                        def entryPoints = ['app.py', 'main.py', 'index.js', 'app.js', 'main.js', 'app.rb']
                        def entryPoint = entryPoints.find { new File(appDir, it).exists() }
                        
                        if (entryPoint) {
                            println "Found entry point: $entryPoint"
                            def file = new File(appDir, entryPoint)
                            def process
                            
                            switch (entryPoint) {
                                case ~/.*\.py/:
                                    process = ["python", file.name].execute(null, appDir)
                                    break
                                case ~/.*\.js/:
                                    process = ["node", file.name].execute(null, appDir)
                                    break
                                case ~/.*\.rb/:
                                    process = ["ruby", file.name].execute(null, appDir)
                                    break
                                default:
                                    println "Unsupported file type: $entryPoint"
                                    return
                            }
                            
                            process.consumeProcessOutput(System.out, System.err)
                            process.waitFor()
                        }
                    }
                    
                    // Mark message as read
                    msg.setFlag(Flags.Flag.SEEN, true)
                    
                } catch (Exception e) {
                    println "Error processing message: ${e.message}"
                    e.printStackTrace()
                }
            }
        } else {
            println "No new messages"
        }
        
        inbox.close(false)
        store.close()
        
    } catch (Exception e) {
        println "Error checking email: ${e.message}"
        e.printStackTrace()
    }
}

// Main loop
while (true) {
    try {
        checkEmails()
    } catch (Exception e) {
        println "Error in main loop: ${e.message}"
        e.printStackTrace()
    }
    
    // Wait for 30 seconds before checking again
    Thread.sleep(30000)
}
