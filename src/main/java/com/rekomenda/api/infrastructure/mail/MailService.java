package com.rekomenda.api.infrastructure.mail;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String resetLink) {
        var subject = "Rekomenda — Redefinição de Senha";
        var body = buildPasswordResetBody(userName, resetLink);
        sendHtmlEmail(toEmail, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage(), ex);
        }
    }

    private String buildPasswordResetBody(String userName, String resetLink) {
        return """
                <html>
                  <body style="font-family: sans-serif; color: #333;">
                    <h2>Olá, %s!</h2>
                    <p>Recebemos uma solicitação para redefinir a senha da sua conta Rekomenda.</p>
                    <p>Clique no link abaixo para criar uma nova senha. O link expira em <strong>2 horas</strong>.</p>
                    <p>
                      <a href="%s" style="background:#6200ea;color:#fff;padding:12px 24px;border-radius:4px;text-decoration:none;">
                        Redefinir Senha
                      </a>
                    </p>
                    <p>Se você não solicitou isso, ignore este e-mail.</p>
                  </body>
                </html>
                """.formatted(userName, resetLink);
    }
}
