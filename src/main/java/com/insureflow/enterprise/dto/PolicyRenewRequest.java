package com.insureflow.enterprise.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PolicyRenewRequest {

    @NotNull(message = "New end date is required")
    private LocalDate newEndDate;

    @DecimalMin(value = "0.01", message = "Updated premium amount must be positive")
    private BigDecimal updatedPremiumAmount;
}
