package com.group5.ems.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Builder
@Data
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private String action;          // từ auditLog.getAction()
    private String entityName;      // auditLog.getEntityName()
    private Long entityId;          // auditLog.getEntityId()
    private String actorName;       // auditLog.getPerformer().getFullName()
    private String actorEmail;      // auditLog.getPerformer().getEmail()
    private String createdAt;       // format LocalDateTime -> String
}