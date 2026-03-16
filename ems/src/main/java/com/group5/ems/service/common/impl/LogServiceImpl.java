package com.group5.ems.service.common.impl;

import com.group5.ems.dto.request.AuditLogDTO;
import com.group5.ems.entity.AuditLog;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.AuditLogRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void log(AuditAction action, AuditEntityType entityType, Long entityId) {
        log(action, entityType, entityId, resolveCurrentUserId());
    }

    @Override
    @Transactional
    public void log(AuditAction action, AuditEntityType entityType, Long entityId, Long performedBy) {
        if (action == null) {
            throw new IllegalArgumentException("Audit action is required");
        }
        if (entityType == null) {
            throw new IllegalArgumentException("Audit entity type is required");
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action.name());
        auditLog.setEntityName(entityType.name());
        auditLog.setEntityId(entityId);
        auditLog.setPerformedBy(performedBy);

        auditLogRepository.save(auditLog);
    }

    @Override
    public AuditLogDTO toAuditLogDTO(AuditLog auditLog) {
        AuditLogDTO auditLogDTO = AuditLogDTO
                .builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .entityName(auditLog.getEntityName())
                .entityId(auditLog.getEntityId())
                .actorName(auditLog.getPerformer() != null ? auditLog.getPerformer().getFullName() : "System")
                .actorEmail(auditLog.getPerformer() != null ? auditLog.getPerformer().getEmail() : null)
                .createdAt(auditLog.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .build();
        return auditLogDTO;
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return userRepository.findByUsername(authentication.getName())
                .map(user -> user.getId())
                .orElse(null);
    }
}
