package com.insureflow.enterprise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponse {
    private Long id;
    private String claimNumber;
    private Long policyId;
    private String policyNumber;
    private BigDecimal claimAmount;
    private String description;
    private String status;
    private String notes;
    private List<String> documentPaths;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ClaimHistoryDto> history;
}
