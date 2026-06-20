package com.insureflow.enterprise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimHistoryDto {
    private Long id;
    private String fromStatus;
    private String toStatus;
    private String action;
    private String notes;
    private String changedBy;
    private LocalDateTime changedAt;
}
