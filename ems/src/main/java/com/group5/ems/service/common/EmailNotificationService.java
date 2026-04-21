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
            EmailTemplate template = emailTemplateRepository.findByCode("LEAVE_APPROVED").orElse(null);
            
            if (template == null) {
                System.err.println("LEAVE_APPROVED email template not found in database");
                return;
            }
            
            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("employeeName", employee.getFullName());
            vars.put("leaveType", request.getLeaveType() != null ? request.getLeaveType() : "Leave");
            vars.put("leaveFrom", request.getLeaveFrom() != null ? request.getLeaveFrom().toString() : "");
            vars.put("leaveTo", request.getLeaveTo() != null ? request.getLeaveTo().toString() : "");
            
            String duration = "";
            if (request.getLeaveFrom() != null && request.getLeaveTo() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(request.getLeaveFrom(), request.getLeaveTo()) + 1;
                duration = days + " day(s)";
            }
            vars.put("duration", duration);
            vars.put("approverName", approver != null ? approver.getFullName() : "HR Manager");
            vars.put("approvedOn", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            
            String subject = replacePlaceholders(template.getSubject(), vars);
            String body = replacePlaceholders(template.getBody(), vars);
            
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(employee.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            
            saveEmailLog(employee.getEmail(), "LEAVE_APPROVED", "SUCCESS");
            
        } catch (Exception e) {
            System.err.println("Failed to send approval notification: " + e.getMessage());
            if (request.getEmployee() != null && request.getEmployee().getUser() != null) {
                saveEmailLog(request.getEmployee().getUser().getEmail(), "LEAVE_APPROVED", "FAILED");
            }
        }
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
            EmailTemplate template = emailTemplateRepository.findByCode("LEAVE_REJECTED").orElse(null);
            
            if (template == null) {
                System.err.println("LEAVE_REJECTED email template not found in database");
                return;
            }
            
            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("employeeName", employee.getFullName());
            vars.put("leaveType", request.getLeaveType() != null ? request.getLeaveType() : "Leave");
            vars.put("leaveFrom", request.getLeaveFrom() != null ? request.getLeaveFrom().toString() : "");
            vars.put("leaveTo", request.getLeaveTo() != null ? request.getLeaveTo().toString() : "");
            vars.put("approverName", approver != null ? approver.getFullName() : "HR Manager");
            vars.put("approvedOn", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            vars.put("reason", reason != null ? reason : "No reason provided");
            
            String subject = replacePlaceholders(template.getSubject(), vars);
            String body = replacePlaceholders(template.getBody(), vars);
            
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(employee.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            
            saveEmailLog(employee.getEmail(), "LEAVE_REJECTED", "SUCCESS");
            
        } catch (Exception e) {
            System.err.println("Failed to send rejection notification: " + e.getMessage());
            if (request.getEmployee() != null && request.getEmployee().getUser() != null) {
                saveEmailLog(request.getEmployee().getUser().getEmail(), "LEAVE_REJECTED", "FAILED");
            }
        }
    }
}
