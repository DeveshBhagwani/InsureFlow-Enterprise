package com.insureflow.enterprise.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NomineeDto {
    private Long id;

    @NotBlank(message = "Nominee name is required")
    private String name;

    @NotBlank(message = "Relationship is required")
    private String relationship;

    @NotNull(message = "Percentage allocation is required")
    @DecimalMin(value = "0.01", message = "Percentage allocation must be at least 0.01")
    @DecimalMax(value = "100.00", message = "Percentage allocation cannot exceed 100.00")
    private BigDecimal percentage;

    private String phone;
}
