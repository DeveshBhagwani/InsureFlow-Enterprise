package com.insureflow.enterprise.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumCalculationRequest {

    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be at least 0")
    @Max(value = 120, message = "Age cannot exceed 120")
    private Integer age;

    @NotBlank(message = "Policy type is required (HEALTH, VEHICLE, LIFE)")
    private String policyType;

    @NotNull(message = "Risk score is required")
    @Min(value = 0, message = "Risk score must be at least 0")
    @Max(value = 100, message = "Risk score cannot exceed 100")
    private Integer riskScore;

    @NotNull(message = "Existing claims count is required")
    @Min(value = 0, message = "Existing claims count must be at least 0")
    private Integer existingClaims;

    private String occupation;
}
