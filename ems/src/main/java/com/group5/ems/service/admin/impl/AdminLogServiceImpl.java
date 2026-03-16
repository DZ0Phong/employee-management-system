package com.group5.ems.service.admin.impl;

import com.group5.ems.dto.request.AuditLogDTO;
import com.group5.ems.entity.AuditLog;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.repository.AuditLogRepository;
import com.group5.ems.service.admin.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLogServiceImpl implements AdminLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public Page<AuditLogDTO> getLogs(String keyword,
                                     String actionFilter,
                                     String dateFrom,
                                     String dateTo,
                                     String sortField,
                                     String sortDir,
                                     int page,
                                     int pageSize) {
        if (page < 0) {
            page = 0;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }

        if (sortField == null || (!"createdAt".equals(sortField) && !"action".equals(sortField) && !"entityName".equals(sortField))) {
            sortField = "createdAt";
        }
        if (sortDir == null || (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir))) {
            sortDir = "desc";
        }

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Sort sort = Sort.by(direction, sortField);

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String normalizedAction = actionFilter == null ? "" : actionFilter.trim().toUpperCase(Locale.ROOT);
        LocalDate fromDate = parseDate(dateFrom);
        LocalDate toDate = parseDate(dateTo);

        List<AuditLog> filteredLogs = auditLogRepository.findAll(sort).stream()
                .filter(log -> matchesKeyword(log, normalizedKeyword))
                .filter(log -> matchesAction(log, normalizedAction))
                .filter(log -> matchesDateRange(log, fromDate, toDate))
                .toList();

        int start = Math.min(page * pageSize, filteredLogs.size());
        int end = Math.min(start + pageSize, filteredLogs.size());

        List<AuditLogDTO> pageContent = filteredLogs.subList(start, end).stream()
                .map(this::toAuditLogDTO)
                .toList();

        return new PageImpl<>(pageContent, PageRequest.of(page, pageSize, sort), filteredLogs.size());
    }

    @Override
    public List<AuditLogDTO> getRecentLogs(int limit) {
        return auditLogRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .limit(limit)
                .map(this::toAuditLogDTO)
                .toList();
    }

    @Override
    public long countTotal() {
        return auditLogRepository.count();
    }

    @Override
    public long countCreate() {
        return auditLogRepository.countByAction(AuditAction.CREATE.name());
    }

    @Override
    public long countUpdateDelete() {
        return auditLogRepository.countByAction(AuditAction.UPDATE.name())
                + auditLogRepository.countByAction(AuditAction.DELETE.name());
    }

    @Override
    public long countToday() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return auditLogRepository.countByCreatedAtBetween(start, end);
    }

    @Override
    @Transactional
    public void deleteLog(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Log id is required");
        }
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Log entry not found"));
        auditLogRepository.delete(log);
    }

    @Override
    @Transactional
    public void clearOlderThanDays(int olderThanDays) {
        if (olderThanDays <= 0) {
            throw new IllegalArgumentException("olderThanDays must be greater than 0");
        }
        LocalDateTime cutoff = LocalDate.now().minusDays(olderThanDays).atStartOfDay();
        auditLogRepository.deleteByCreatedAtBefore(cutoff);
    }

    @Override
    public AuditLogDTO toAuditLogDTO(AuditLog auditLog) {
        String createdAt = auditLog.getCreatedAt() != null
                ? auditLog.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                : "—";

        return AuditLogDTO.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .entityName(auditLog.getEntityName())
                .entityId(auditLog.getEntityId())
                .actorName(auditLog.getPerformer() != null ? auditLog.getPerformer().getFullName() : "System")
                .actorEmail(auditLog.getPerformer() != null ? auditLog.getPerformer().getEmail() : null)
                .createdAt(createdAt)
                .build();
    }

    private boolean matchesKeyword(AuditLog log, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }

        String actorName = log.getPerformer() != null && log.getPerformer().getFullName() != null
                ? log.getPerformer().getFullName().toLowerCase(Locale.ROOT)
                : "";
        String actorEmail = log.getPerformer() != null && log.getPerformer().getEmail() != null
                ? log.getPerformer().getEmail().toLowerCase(Locale.ROOT)
                : "";
        String action = log.getAction() != null ? log.getAction().toLowerCase(Locale.ROOT) : "";
        String entityName = log.getEntityName() != null ? log.getEntityName().toLowerCase(Locale.ROOT) : "";
        String entityId = log.getEntityId() != null ? String.valueOf(log.getEntityId()) : "";

        return action.contains(keyword)
                || entityName.contains(keyword)
                || entityId.contains(keyword)
                || actorName.contains(keyword)
                || actorEmail.contains(keyword);
    }

    private boolean matchesAction(AuditLog log, String actionFilter) {
        if (actionFilter == null || actionFilter.isEmpty()) {
            return true;
        }
        return log.getAction() != null && log.getAction().equalsIgnoreCase(actionFilter);
    }

    private boolean matchesDateRange(AuditLog log, LocalDate fromDate, LocalDate toDate) {
        if (log.getCreatedAt() == null) {
            return fromDate == null && toDate == null;
        }

        LocalDate createdDate = log.getCreatedAt().toLocalDate();
        if (fromDate != null && createdDate.isBefore(fromDate)) {
            return false;
        }
        if (toDate != null && createdDate.isAfter(toDate)) {
            return false;
        }
        return true;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }
}
