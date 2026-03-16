package com.group5.ems.service.guest;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.group5.ems.entity.EmailTemplate;
import com.group5.ems.repository.EmailTemplateRepository;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateRepository emailTemplateRepository;

    public void sendContactEmail(
            String name,
            String email,
            String phone,
            String topic,
            String message) {

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo("trantrungd83@gmail.com");
        mail.setSubject("New Contact Message: " + topic);
        mail.setText(
            "Name: " + name + "\n" +
            "Email: " + email + "\n" +
            "Phone: " + phone + "\n\n" +
            "Message:\n" + message);
        mailSender.send(mail);
    }

    // ── Gửi email từ template code + biến thay thế ──
    public void sendFromTemplate(String toEmail, String templateCode, Map<String, String> variables) {

        EmailTemplate template = emailTemplateRepository
                .findByCode(templateCode)
                .orElseThrow(() -> new RuntimeException("Email template not found: " + templateCode));

        String subject = replacePlaceholders(template.getSubject(), variables);
        String body    = replacePlaceholders(template.getBody(), variables);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    // Thay {{placeholder}} bằng giá trị thực
    private String replacePlaceholders(String text, Map<String, String> variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            text = text.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return text;
    }
}