package org.ftf.koifishveterinaryservicecenter.service.emailservice;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class EmailService {


    @Value("${sender.email}")
    private String senderEmail;

    private final JavaMailSender mailSender;


    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTextEmail(String to, String subject, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(senderEmail);
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        mailSender.send(mailMessage);
    }

    @Async // execute method asynchronously
    public void sendAppointmentBills(String to, String subject) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            Resource resource = new ClassPathResource("/templates/bills.html");
            String htmlContent = new String(resource.getInputStream().readAllBytes());

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
