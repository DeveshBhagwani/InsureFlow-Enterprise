package com.insureflow.enterprise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryResponse {
    private long activePoliciesCount;
    private long totalClaimsCount;
    private BigDecimal totalRevenue;
    private long totalCustomersCount;
}
