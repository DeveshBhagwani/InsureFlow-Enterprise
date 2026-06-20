package com.insureflow.enterprise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String paymentNumber;
    private Long policyId;
    private String policyNumber;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String transactionType;
    private String notes;
    private Long originalPaymentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
