package com.insureflow.enterprise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {
    private Long id;
    private String policyNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String policyType;
    private String status;
    private BigDecimal premiumAmount;
    private BigDecimal coverageAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Subclass details
    private HealthDetailsResponse healthDetails;
    private VehicleDetailsResponse vehicleDetails;
    private LifeDetailsResponse lifeDetails;

    // Transition History
    private List<PolicyHistoryDto> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthDetailsResponse {
        private BigDecimal deductible;
        private BigDecimal coPayPercentage;
        private String preExistingConditions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleDetailsResponse {
        private String vehicleMake;
        private String vehicleModel;
        private String licensePlate;
        private String vehicleValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LifeDetailsResponse {
        private Integer termYears;
        private boolean smoker;
        private String medicalHistorySummary;
    }
}
