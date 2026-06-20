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
public class PolicyReportResponse {
    private String policyType;
    private long count;
    private BigDecimal totalPremium;
}
