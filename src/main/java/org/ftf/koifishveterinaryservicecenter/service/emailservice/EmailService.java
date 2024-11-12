package org.ftf.koifishveterinaryservicecenter.service.emailservice;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.ftf.koifishveterinaryservicecenter.dto.appointment.AppointmentDetailsDto;
import org.ftf.koifishveterinaryservicecenter.entity.Appointment;
import org.ftf.koifishveterinaryservicecenter.entity.User;
import org.ftf.koifishveterinaryservicecenter.mapper.AppointmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
@Slf4j
public class EmailService {


    @Value("${sender.email}")
    private String senderEmail;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Autowired
    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }


    public void sendTextEmail(String to, String subject, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(senderEmail);
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        mailSender.send(mailMessage);
    }

    public void sendAppointmentBills(String to, String subject, Appointment appointment) {
        try {
            Context context = new Context();
            AppointmentDetailsDto detailsDto = AppointmentMapper.INSTANCE.convertedToAppointmentDetailsDto(appointment);
            context.setVariable("appointment", detailsDto);

            // resolve resource by Thymeleaf
            String htmlContent = templateEngine.process("bills", context);

            // create the email message
            sendEmail(to, subject, htmlContent);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }


    @Async
    public void sendEmailForCancelingAppointment(String to, String subject, User user) {
        try {
            Context context = new Context();
            context.setVariable("customer", user);

            String htmlContent = templateEngine.process("cancel", context);

            sendEmail(to, subject, htmlContent);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(senderEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
