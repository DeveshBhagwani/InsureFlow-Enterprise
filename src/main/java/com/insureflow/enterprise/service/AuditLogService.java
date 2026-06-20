package com.insureflow.enterprise.service;

import com.insureflow.enterprise.dto.AuditLogResponse;
import com.insureflow.enterprise.model.AuditLog;
import com.insureflow.enterprise.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listAuditLogs(String query, int page, int size) {
        checkAdminAccess();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<AuditLog> logs = auditLogRepository.searchAuditLogs(query, pageable);
        return logs.map(this::mapToResponse);
    }

    private void checkAdminAccess() {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new AccessDeniedException("Access Denied: Admin privileges required to view audit logs.");
        }
    }

    private AuditLogResponse mapToResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .userEmail(a.getUserEmail())
                .action(a.getAction())
                .entityName(a.getEntityName())
                .entityId(a.getEntityId())
                .beforeState(a.getBeforeState())
                .afterState(a.getAfterState())
                .timestamp(a.getTimestamp())
                .build();
    }
}
