package com.insureflow.enterprise.strategy;

import com.insureflow.enterprise.model.PolicyType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PremiumCalculationInput {
    private int age;
    private PolicyType policyType;
    private int riskScore;
    private int existingClaims;
    private String occupation;
}
