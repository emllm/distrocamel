package com.emllm

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.component.mail.MailMessage
import org.apache.commons.mail.util.MimeMessageParser

import javax.mail.internet.MimeMessage
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.Message
import javax.mail.Transport
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.nio.file.Files
import java.nio.file.Paths

groovy.transform.CompileStatic
class EmailProcessingRoute extends RouteBuilder {
    
    @Override
    void configure() throws Exception {
        // Get configuration from environment variables with defaults
        def imapUser = System.getenv('IMAP_USER') ?: 'llm@example.com'
        def imapPass = System.getenv('IMAP_PASSWORD') ?: 'password'
        def imapHost = System.getenv('IMAP_HOST') ?: 'localhost'
        def imapPort = System.getenv('IMAP_PORT') ?: '1143'
        def smtpHost = System.getenv('SMTP_HOST') ?: 'localhost'
        def smtpPort = System.getenv('SMTP_PORT') ?: '1025'
        def ollamaUrl = System.getenv('OLLAMA_BASE_URL') ?: 'http://localhost:11434'

        // Configure the IMAP endpoint
        def imapEndpoint = "imap://${imapUser}@${imapHost}:${imapPort}?password=${imapPass}&delete=false&unseen=true&consumer.delay=10000"
        
        from(imapEndpoint)
            .routeId("email-processing-route")
            .process(new EmailProcessor(ollamaUrl: ollamaUrl, smtpHost: smtpHost, smtpPort: smtpPort))
            .log("Processed email")
            .end()
    }
}

@CompileStatic
class EmailProcessor implements Processor {
    String ollamaUrl
    String smtpHost
    String smtpPort
    
    @Override
    void process(Exchange exchange) throws Exception {
        def mailMessage = exchange.getIn().getBody(MailMessage.class)
        def message = mailMessage.message
        
        // Extract email content
        def from = message.from[0].toString()
        def subject = message.subject
        def content = getTextFromMessage(message)
        
        println "Processing email from: $from, Subject: $subject"
        
        // Call LLM to generate application
        def appDetails = generateApplication(content)
        
        // Create and send response
        sendResponse(from, subject, appDetails)
    }
    
    private String getTextFromMessage(Message message) {
        return new MimeMessageParser((MimeMessage)message).parse().plainContent
    }
    
    private Map generateApplication(String requirements) {
        // In a real implementation, this would call the LLM API
        // For now, we'll return a mock response
        return [
            "name": "generated-app",
            "description": "Generated application based on: $requirements",
            "files": [
                ["name": "app.py", "content": "# Generated application\nprint('Hello, World!') "],
                ["name": "README.md", "content": "# Generated Application\n\nThis application was generated based on your requirements."]
            ]
        ]
    }
    
    private void sendResponse(String to, String originalSubject, Map appDetails) {
        def session = Session.getInstance([
            'mail.smtp.host': smtpHost,
            'mail.smtp.port': smtpPort,
            'mail.smtp.auth': 'false',
            'mail.smtp.starttls.enable': 'false'
        ])
        
        def message = new MimeMessage(session)
        message.setFrom(new InternetAddress('llm@example.com'))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
        message.setSubject("RE: $originalSubject")
        
        // Create multipart message
        def multipart = new javax.mail.internet.MimeMultipart()
        
        // Add text part
        def textPart = new javax.mail.internet.MimeBodyPart()
        textPart.setText("Here is your generated application. Please find the attached files.")
        multipart.addBodyPart(textPart)
        
        // Add files as attachments
        appDetails.files.each { file ->
            def filePart = new javax.mail.internet.MimeBodyPart()
            filePart.setFileName(file.name)
            filePart.setContent(file.content, "text/plain")
            multipart.addBodyPart(filePart)
        }
        
        message.setContent(multipart)
        
        // Send the message
        Transport.send(message)
        println "Sent response to: $to"
    }
}
