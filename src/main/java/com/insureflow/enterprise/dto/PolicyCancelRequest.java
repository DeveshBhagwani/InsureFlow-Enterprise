package com.insureflow.enterprise.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PolicyCancelRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
