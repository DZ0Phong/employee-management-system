package com.group5.ems.service.admin;

import com.group5.ems.dto.request.AuditLogDTO;
import com.group5.ems.entity.AuditLog;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminLogService {

    Page<AuditLogDTO> getLogs(String keyword,
                              String actionFilter,
                              String dateFrom,
                              String dateTo,
                              String sortField,
                              String sortDir,
                              int page,
                              int pageSize);

    List<AuditLogDTO> getRecentLogs(int limit);

    long countTotal();

    long countCreate();

    long countUpdateDelete();

    long countToday();

    void deleteLog(Long id);

    void clearOlderThanDays(int olderThanDays);

    AuditLogDTO toAuditLogDTO(AuditLog auditLog);
}
