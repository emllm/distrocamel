@Grab('javax.mail:javax.mail-api:1.6.2')
@Grab('com.sun.mail:javax.mail:1.6.2')

import javax.mail.*
import javax.mail.internet.*
import java.util.Properties

// Get environment variables
def smtpHost = System.getenv('SMTP_HOST') ?: 'localhost'
def smtpPort = System.getenv('SMTP_PORT') ?: '1025'
def fromEmail = System.getenv('FROM_EMAIL') ?: 'bot@example.com'
def toEmail = System.getenv('TO_EMAIL') ?: 'llm@example.com'

def prompts = [
    "Create a simple Node.js web server that shows the current time",
    "Generate a Python script that reads a CSV file and calculates statistics",
    "Build a React component that displays a countdown timer",
    "Create a shell script that monitors disk usage and sends an alert if it's above 90%",
    "Generate a Python FastAPI endpoint that returns a random quote"
]

// Email properties
def props = new Properties()
props.put('mail.smtp.host', smtpHost)
props.put('mail.smtp.port', smtpPort)
props.put('mail.smtp.auth', 'false')
props.put('mail.smtp.starttls.enable', 'false')

def session = Session.getDefaultInstance(props)

// Send a random prompt every minute
while (true) {
    try {
        def message = new MimeMessage(session)
        message.setFrom(new InternetAddress(fromEmail))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
        message.setSubject("Application Generation Request")
        
        // Select a random prompt
        def randomPrompt = prompts[new Random().nextInt(prompts.size())]
        message.setText("Please generate an application with the following requirements:\n\n" + randomPrompt)
        
        // Send the message
        Transport.send(message)
        println("Sent request: $randomPrompt")
    } catch (Exception e) {
        println "Error sending email: ${e.message}"
    }
    
    // Wait for 1 minute
    Thread.sleep(60000)
}
