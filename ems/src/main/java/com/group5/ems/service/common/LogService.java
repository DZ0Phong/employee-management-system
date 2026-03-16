package com.group5.ems.service.common;

import com.group5.ems.dto.request.AuditLogDTO;
import com.group5.ems.entity.AuditLog;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;

public interface LogService {

    /**
     * Ghi audit log và tự lấy user hiện tại từ SecurityContext.
     *
     * Cách dùng trong service nghiệp vụ:
     * logService.log(AuditAction.CREATE, AuditEntityType.USER, savedUser.getId());
     * logService.log(AuditAction.UPDATE, AuditEntityType.DEPARTMENT, department.getId());
     */
    void log(AuditAction action, AuditEntityType entityType, Long entityId);

    /**
     * Ghi audit log khi đã biết rõ user thực hiện thao tác.
     *
     * Cách dùng:
     * logService.log(AuditAction.DELETE, AuditEntityType.USER, userId, currentUserId);
     * logService.log(AuditAction.LOGIN, AuditEntityType.AUTH, userId, userId);
     */
    void log(AuditAction action, AuditEntityType entityType, Long entityId, Long performedBy);

    AuditLogDTO toAuditLogDTO(AuditLog auditLog);
}
