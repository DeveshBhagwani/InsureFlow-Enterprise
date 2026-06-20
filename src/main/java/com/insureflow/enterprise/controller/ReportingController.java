package com.insureflow.enterprise.controller;

import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Reporting Management", description = "Endpoints for compiling reporting metrics and analytics")
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
public class ReportingController {

    private final ReportingService reportingService;

    @Operation(summary = "Get consolidated report summary (active policies, claims, customers, net revenue)")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getSummaryReport() {
        ReportSummaryResponse summary = reportingService.getSummaryReport();
        return ResponseEntity.ok(ApiResponse.success(summary, "Summary report retrieved successfully"));
    }

    @Operation(summary = "Get active policy metrics grouped by type")
    @GetMapping("/policies")
    public ResponseEntity<ApiResponse<List<PolicyReportResponse>>> getPolicyReport() {
        List<PolicyReportResponse> policyReport = reportingService.getPolicyReport();
        return ResponseEntity.ok(ApiResponse.success(policyReport, "Policy report retrieved successfully"));
    }

    @Operation(summary = "Get claim metrics grouped by status")
    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<List<ClaimReportResponse>>> getClaimReport() {
        List<ClaimReportResponse> claimReport = reportingService.getClaimReport();
        return ResponseEntity.ok(ApiResponse.success(claimReport, "Claim report retrieved successfully"));
    }

    @Operation(summary = "Get net revenue metrics (successful payments minus refunds)")
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport() {
        RevenueReportResponse revenueReport = reportingService.getRevenueReport();
        return ResponseEntity.ok(ApiResponse.success(revenueReport, "Revenue report retrieved successfully"));
    }

    @Operation(summary = "Get customer growth sign-up trends grouped by registration date")
    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<List<CustomerGrowthResponse>>> getCustomerGrowthReport() {
        List<CustomerGrowthResponse> growthReport = reportingService.getCustomerGrowthReport();
        return ResponseEntity.ok(ApiResponse.success(growthReport, "Customer growth report retrieved successfully"));
    }
}
