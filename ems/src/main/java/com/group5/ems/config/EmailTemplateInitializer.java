package com.group5.ems.config;

import com.group5.ems.entity.EmailTemplate;
import com.group5.ems.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures required email templates exist in the database.
 * This satisfies the requirement to "Write template for this".
 */
@Configuration
@RequiredArgsConstructor
public class EmailTemplateInitializer implements CommandLineRunner {

    private final EmailTemplateRepository emailTemplateRepository;

    @Override
    public void run(String... args) {
        initializeReportPublishedTemplate();
    }

    private void initializeReportPublishedTemplate() {
        EmailTemplate template = emailTemplateRepository.findByCode("REPORT_PUBLISHED")
                .orElse(new EmailTemplate());
        
        template.setCode("REPORT_PUBLISHED");
        template.setSubject("""
            New HR Report Published: {{reportTitle}}
            """);
        template.setBody("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>HR Report Notification</title>
            </head>
            <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0;">
                <div style="max-width: 600px; margin: 20px auto; padding: 20px; border: 1px solid #e5e7eb; border-radius: 12px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                    
                    <!-- Header -->
                    <div style="text-align: center; border-bottom: 2px solid #4f46e5; padding-bottom: 20px; margin-bottom: 25px;">
                        <h1 style="color: #4f46e5; margin: 0; font-size: 24px;">EMS HR Analytics</h1>
                        <p style="color: #6b7280; font-size: 14px; margin: 5px 0 0 0;">Automated Report Notification Service</p>
                    </div>
    
                    <!-- Content -->
                    <div style="padding: 0 10px;">
                        <p style="font-size: 16px;">Dear <strong>{{managerName}}</strong>,</p>
                        <p>A new finalized HR report has been successfully published. Please find the report attached to this email for your preview and download.</p>
                        
                        <div style="background-color: #f9fafb; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 5px solid #4f46e5;">
                            <h3 style="margin: 0 0 10px 0; color: #374151; font-size: 16px;">Report Details</h3>
                            <table style="width: 100%; border-collapse: collapse;">
                                <tr>
                                    <td style="padding: 5px 0; color: #6b7280; width: 120px;">Title:</td>
                                    <td style="padding: 5px 0; color: #111827; font-weight: 600;">{{reportTitle}}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 5px 0; color: #6b7280;">Type:</td>
                                    <td style="padding: 5px 0; color: #111827;">{{reportType}}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 5px 0; color: #6b7280;">Published:</td>
                                    <td style="padding: 5px 0; color: #111827;">{{publishedDate}}</td>
                                </tr>
                            </table>
                        </div>
                        
                        <!-- Remarks / Executive Summary -->
                        <div style="margin-top: 25px;">
                            <h3 style="font-size: 16px; color: #374151; margin-bottom: 10px;">Executive Summary</h3>
                            <div style="background-color: #f3f4f6; border-radius: 8px; padding: 15px; color: #4b5563; font-style: italic; border: 1px dashed #d1d5db;">
                                {{remarks}}
                            </div>
                        </div>
                    </div>
    
                    <!-- Footer -->
                    <div style="margin-top: 40px; padding-top: 20px; border-top: 1px solid #e5e7eb; text-align: center; color: #9ca3af; font-size: 12px;">
                        <p style="margin: 0;">This email was sent to notify you of a report publication in the EMS system.</p>
                        <p style="margin: 5px 0;">Please do not reply to this automated message.</p>
                        <p style="margin: 15px 0 0 0;">&copy; 2026 EMS HR Information System. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """);
        emailTemplateRepository.save(template);
    }
}
