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
