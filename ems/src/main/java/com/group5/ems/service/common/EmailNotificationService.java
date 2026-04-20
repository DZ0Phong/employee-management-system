package com.group5.ems.service.common;

import com.group5.ems.entity.Request;
import com.group5.ems.entity.User;
import com.group5.ems.repository.*;
import com.group5.ems.service.hr.HrBackblazeStorageService;
import com.group5.ems.entity.EmailTemplate;
import com.group5.ems.entity.HrReport;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Email Notification Service
 * Handles sending email notifications for critical requests and other events
 * 
 * NOTE: This is a placeholder implementation. In production, you would:
 * 1. Configure Spring Mail (spring-boot-starter-mail)
 * 2. Set up SMTP server credentials in application.properties
 * 3. Use JavaMailSender to send actual emails
 * 4. Create HTML email templates
 */
@Service
public class EmailNotificationService {

    @Autowired
    private RequestRepository requestRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmailTemplateRepository emailTemplateRepository;

    @Autowired
    private EmailLogRepository emailLogRepository;

    @Autowired
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @Autowired
    private LogService logService;

    @Autowired
    private HrBackblazeStorageService backblazeStorageService;

    /**
     * Send notification to HR Manager when a report is finalized and published
     */
    public void sendReportPublishedNotification(HrReport report) {
        try {
            // 1. Get recipients (HR Managers)
            List<com.group5.ems.entity.Employee> hrManagers = employeeRepository
                    .findEmployeesByRoleCodes(List.of("HR_MANAGER"));

            if (hrManagers.isEmpty()) {
                System.err.println("No HR Managers found to notify for report: " + report.getTitle());
                return;
            }

            // 2. Fetch template
            EmailTemplate template = emailTemplateRepository.findByCode("REPORT_PUBLISHED")
                    .orElse(null);

            if (template == null) {
                System.err.println("REPORT_PUBLISHED email template not found");
                return;
            }

            // 3. Prepare variables
            String reportDate = report.getGeneratedAt() != null 
                    ? report.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "";

            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("reportTitle", report.getTitle());
            vars.put("reportType", report.getReportType().toString());
            vars.put("publishedDate", reportDate);
            vars.put("remarks", report.getRemarks() != null ? report.getRemarks() : "No executive summary provided.");
            vars.put("reportId", String.valueOf(report.getId()));

            // 4. Fetch PDF bytes from Backblaze
            byte[] pdfBytes = null;
            if (report.getFilePath() != null) {
                try {
                    pdfBytes = backblazeStorageService.downloadReport(report.getFilePath()).orElse(null);
                } catch (Exception e) {
                    System.err.println("Failed to fetch PDF for email attachment: " + e.getMessage());
                }
            }

            // 5. Generate filename: Report_{Type}_{Title}_{Date}.pdf
            String dateSuffix = report.getGeneratedAt() != null
                    ? report.getGeneratedAt().format(DateTimeFormatter.ofPattern("ddMMyyyy"))
                    : java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
            
            String safeTitle = report.getTitle().replaceAll("[^a-zA-Z0-9]", "_");
            String fileName = String.format("Report_%s_%s_%s.pdf", 
                    report.getReportType(), safeTitle, dateSuffix);

            // 6. Send to each manager
            for (com.group5.ems.entity.Employee manager : hrManagers) {
                try {
                    if (manager.getUser() == null) continue;
                    
                    vars.put("managerName", manager.getUser().getFullName());
                    sendHtmlEmail(manager.getUser().getEmail(), template, vars, pdfBytes, fileName, "application/pdf");
                    
                    // Log to DB
                    saveEmailLog(manager.getUser().getEmail(), "REPORT_PUBLISHED", "SUCCESS");
                } catch (Exception e) {
                    String email = manager.getUser() != null ? manager.getUser().getEmail() : "unknown";
                    System.err.println("Failed to send report notification to " + email);
                    e.printStackTrace();
                    saveEmailLog(email, "REPORT_PUBLISHED", "FAILED");
                }
            }

            // Global Audit Log for the report publication event
            logService.log(AuditAction.UPDATE, 
                           AuditEntityType.HR_REPORTS, 
                           report.getId());

        } catch (Exception e) {
            System.err.println("Critical error in report notification service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendHtmlEmail(String to, EmailTemplate template, Map<String, String> variables) throws Exception {
        sendHtmlEmail(to, template, variables, null, null, null);
    }

    private void sendHtmlEmail(String to, EmailTemplate template, Map<String, String> variables, byte[] attachment, String fileName, String contentType) throws Exception {
        String subject = replacePlaceholders(template.getSubject(), variables);
        String htmlBody = replacePlaceholders(template.getBody(), variables);

        jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // true = HTML

        if (attachment != null && fileName != null) {
            String mimeType = contentType != null ? contentType : "application/pdf";
            helper.addAttachment(fileName, new ByteArrayDataSource(attachment, mimeType));
        }

        mailSender.send(message);
    }

    private void saveEmailLog(String email, String templateCode, String status) {
        com.group5.ems.entity.EmailLog log = new com.group5.ems.entity.EmailLog();
        log.setRecipientEmail(email);
        log.setTemplateCode(templateCode);
        log.setStatus(status);
        emailLogRepository.save(log);
    }

    private String replacePlaceholders(String text, java.util.Map<String, String> variables) {
        if (text == null) return "";
        for (java.util.Map.Entry<String, String> entry : variables.entrySet()) {
            text = text.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return text;
    }

    /**
     * Send notification for critical requests to HR Manager
     * @param hrManagerId ID of the HR Manager to notify
     * @param criticalCount Number of critical requests
     */
    public void sendCriticalRequestsNotification(Long hrManagerId, int criticalCount) {
        try {
            // Get HR Manager details
            User hrManager = userRepository.findById(hrManagerId)
                    .orElseThrow(() -> new RuntimeException("HR Manager not found"));
            
            // Get critical pending requests
            List<Request> criticalRequests = requestRepository
                    .findByStatusAndPriorityOrderByCreatedAtDesc("PENDING", "CRITICAL");
            
            // Build email content
            String subject = String.format("⚠️ URGENT: %d Critical Request(s) Need Immediate Attention", criticalCount);
            String body = buildCriticalEmailBody(hrManager, criticalRequests);
            
            // TODO: Send actual email
            // In production, use JavaMailSender:
            // mailSender.send(mimeMessage);
            
            // For now, just log it
            System.out.println("=".repeat(80));
            System.out.println("EMAIL NOTIFICATION");
            System.out.println("=".repeat(80));
            System.out.println("To: " + hrManager.getEmail());
            System.out.println("Subject: " + subject);
            System.out.println("-".repeat(80));
            System.out.println(body);
            System.out.println("=".repeat(80));
            
        } catch (Exception e) {
            System.err.println("Failed to send critical notification email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Build email body for critical requests notification
     */
    private String buildCriticalEmailBody(User hrManager, List<Request> criticalRequests) {
        StringBuilder body = new StringBuilder();
        
        body.append("Dear ").append(hrManager.getFullName()).append(",\n\n");
        body.append("You have ").append(criticalRequests.size())
            .append(" CRITICAL request(s) that require immediate attention:\n\n");
        
        int count = 1;
        for (Request request : criticalRequests) {
            body.append(count++).append(". ");
            
            if (request.getEmployee() != null && request.getEmployee().getUser() != null) {
                body.append(request.getEmployee().getUser().getFullName());
            } else {
                body.append("Unknown Employee");
            }
            
            body.append(" - ").append(request.getLeaveType() != null ? request.getLeaveType() : "Request");
            
            if (request.getLeaveFrom() != null && request.getLeaveTo() != null) {
                body.append(" (").append(request.getLeaveFrom())
                    .append(" to ").append(request.getLeaveTo()).append(")");
            }
            
            body.append("\n   Priority Score: ").append(request.getPriorityScore() != null ? request.getPriorityScore() : "N/A");
            body.append("\n   Submitted: ").append(request.getCreatedAt() != null ? request.getCreatedAt() : "N/A");
            body.append("\n   Reason: ").append(request.getContent() != null ? request.getContent() : "No reason provided");
            body.append("\n\n");
        }
        
        body.append("Please review and process these requests as soon as possible.\n\n");
        body.append("View all requests: http://localhost:8080/hrmanager/leave-approval?tab=pending\n\n");
        body.append("Best regards,\n");
        body.append("EMS Notification System");
        
        return body.toString();
    }

    /**
     * Send notification when a request is approved
     */
    public void sendApprovalNotification(Request request, User approver) {
        try {
            if (request.getEmployee() == null || request.getEmployee().getUser() == null) {
                return;
            }
            
            User employee = request.getEmployee().getUser();
            String subject = "✅ Your Leave Request Has Been Approved";
            String body = buildApprovalEmailBody(employee, request, approver);
            
            // TODO: Send actual email
            System.out.println("=".repeat(80));
            System.out.println("APPROVAL NOTIFICATION");
            System.out.println("=".repeat(80));
            System.out.println("To: " + employee.getEmail());
            System.out.println("Subject: " + subject);
            System.out.println("-".repeat(80));
            System.out.println(body);
            System.out.println("=".repeat(80));
            
        } catch (Exception e) {
            System.err.println("Failed to send approval notification: " + e.getMessage());
        }
    }

    /**
     * Build email body for approval notification
     */
    private String buildApprovalEmailBody(User employee, Request request, User approver) {
        StringBuilder body = new StringBuilder();
        
        body.append("Dear ").append(employee.getFullName()).append(",\n\n");
        body.append("Good news! Your leave request has been approved.\n\n");
        body.append("Request Details:\n");
        body.append("- Type: ").append(request.getLeaveType() != null ? request.getLeaveType() : "Leave").append("\n");
        body.append("- From: ").append(request.getLeaveFrom()).append("\n");
        body.append("- To: ").append(request.getLeaveTo()).append("\n");
        body.append("- Duration: ");
        
        if (request.getLeaveFrom() != null && request.getLeaveTo() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    request.getLeaveFrom(), request.getLeaveTo()) + 1;
            body.append(days).append(" day(s)");
        }
        
        body.append("\n- Approved by: ").append(approver != null ? approver.getFullName() : "HR Manager").append("\n");
        body.append("- Approved on: ").append(LocalDateTime.now()).append("\n\n");
        body.append("Enjoy your time off!\n\n");
        body.append("Best regards,\n");
        body.append("HR Department");
        
        return body.toString();
    }

    /**
     * Send notification when a request is rejected
     */
    public void sendRejectionNotification(Request request, User approver, String reason) {
        try {
            if (request.getEmployee() == null || request.getEmployee().getUser() == null) {
                return;
            }
            
            User employee = request.getEmployee().getUser();
            String subject = "❌ Your Leave Request Has Been Rejected";
            String body = buildRejectionEmailBody(employee, request, approver, reason);
            
            // TODO: Send actual email
            System.out.println("=".repeat(80));
            System.out.println("REJECTION NOTIFICATION");
            System.out.println("=".repeat(80));
            System.out.println("To: " + employee.getEmail());
            System.out.println("Subject: " + subject);
            System.out.println("-".repeat(80));
            System.out.println(body);
            System.out.println("=".repeat(80));
            
        } catch (Exception e) {
            System.err.println("Failed to send rejection notification: " + e.getMessage());
        }
    }

    /**
     * Build email body for rejection notification
     */
    private String buildRejectionEmailBody(User employee, Request request, User approver, String reason) {
        StringBuilder body = new StringBuilder();
        
        body.append("Dear ").append(employee.getFullName()).append(",\n\n");
        body.append("We regret to inform you that your leave request has been rejected.\n\n");
        body.append("Request Details:\n");
        body.append("- Type: ").append(request.getLeaveType() != null ? request.getLeaveType() : "Leave").append("\n");
        body.append("- From: ").append(request.getLeaveFrom()).append("\n");
        body.append("- To: ").append(request.getLeaveTo()).append("\n");
        body.append("- Rejected by: ").append(approver != null ? approver.getFullName() : "HR Manager").append("\n");
        body.append("- Rejected on: ").append(LocalDateTime.now()).append("\n\n");
        body.append("Reason for rejection:\n");
        body.append(reason != null ? reason : "No reason provided").append("\n\n");
        body.append("If you have any questions or would like to discuss this decision, ");
        body.append("please contact the HR department.\n\n");
        body.append("Best regards,\n");
        body.append("HR Department");
        
        return body.toString();
    }
}
