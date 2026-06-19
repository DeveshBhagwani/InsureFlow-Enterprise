package com.insureflow.enterprise.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PolicyCreateRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank(message = "Policy type is required (HEALTH, VEHICLE, LIFE)")
    private String policyType;

    @NotNull(message = "Premium amount is required")
    @DecimalMin(value = "0.01", message = "Premium amount must be greater than 0")
    private BigDecimal premiumAmount;

    @NotNull(message = "Coverage amount is required")
    @DecimalMin(value = "0.01", message = "Coverage amount must be greater than 0")
    private BigDecimal coverageAmount;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    // Subclass Specific Details
    @Valid
    private HealthDetails healthDetails;

    @Valid
    private VehicleDetails vehicleDetails;

    @Valid
    private LifeDetails lifeDetails;

    @Data
    public static class HealthDetails {
        @DecimalMin(value = "0.00", message = "Deductible must be positive")
        private BigDecimal deductible;

        @DecimalMin(value = "0.00", message = "Co-pay percentage must be positive")
        private BigDecimal coPayPercentage;

        private String preExistingConditions;
    }

    @Data
    public static class VehicleDetails {
        @NotBlank(message = "Vehicle make is required")
        private String vehicleMake;

        @NotBlank(message = "Vehicle model is required")
        private String vehicleModel;

        @NotBlank(message = "License plate is required")
        private String licensePlate;

        @DecimalMin(value = "0.00", message = "Vehicle value must be positive")
        private BigDecimal vehicleValue;
    }

    @Data
    public static class LifeDetails {
        @NotNull(message = "Term duration in years is required")
        private Integer termYears;

        private boolean smoker;

        private String medicalHistorySummary;
    }
}
