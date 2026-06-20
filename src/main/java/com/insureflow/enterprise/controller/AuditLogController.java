package com.insureflow.enterprise.controller;

import com.insureflow.enterprise.dto.ApiResponse;
import com.insureflow.enterprise.dto.AuditLogResponse;
import com.insureflow.enterprise.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit Logging", description = "Endpoints for retrieving system audit trails and user activity histories")
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "Retrieve system audit logs (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AuditLogResponse> response = auditLogService.listAuditLogs(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Audit logs retrieved successfully"));
    }
}
