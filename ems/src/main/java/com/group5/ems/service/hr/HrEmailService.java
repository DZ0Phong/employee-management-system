package com.group5.ems.service.hr;

import java.util.Map;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.group5.ems.entity.EmailLog;
import com.group5.ems.entity.EmailTemplate;
import com.group5.ems.repository.EmailLogRepository;
import com.group5.ems.repository.EmailTemplateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HrEmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailLogRepository emailLogRepository;

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

    /**
     * Gửi email từ template code + biến thay thế, đồng thời ghi log vào DB.
     *
     * @param toEmail      địa chỉ nhận
     * @param templateCode mã template (vd: INTERVIEW_ASSIGNED, APPLICATION_HIRED, ...)
     * @param variables    map các placeholder → giá trị thực
     */
    public void sendFromTemplate(String toEmail, String templateCode, Map<String, String> variables) {

        EmailTemplate template = emailTemplateRepository
                .findByCode(templateCode)
                .orElseThrow(() -> new RuntimeException("Email template not found: " + templateCode));

        String subject = replacePlaceholders(template.getSubject(), variables);
        String body    = replacePlaceholders(template.getBody(),    variables);

        String status = "SUCCESS";
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            status = "FAILED";
            log.error("Failed to send email [{}] to {}: {}", templateCode, toEmail, ex.getMessage());
        } finally {
            saveLog(toEmail, templateCode, status);
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    /** Thay {{placeholder}} bằng giá trị thực. */
    private String replacePlaceholders(String text, Map<String, String> variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            text = text.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return text;
    }

    /** Ghi một bản ghi vào bảng email_logs. */
    private void saveLog(String recipientEmail, String templateCode, String status) {
        try {
            EmailLog emailLog = new EmailLog();
            emailLog.setRecipientEmail(recipientEmail);
            emailLog.setTemplateCode(templateCode);
            emailLog.setStatus(status);
            emailLogRepository.save(emailLog);
        } catch (Exception ex) {
            log.warn("Could not save email log for [{}] → {}: {}", templateCode, recipientEmail, ex.getMessage());
        }
    }
}