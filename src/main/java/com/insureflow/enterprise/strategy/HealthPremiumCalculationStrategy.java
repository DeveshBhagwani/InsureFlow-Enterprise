package com.insureflow.enterprise.strategy;

import com.insureflow.enterprise.model.PolicyType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HealthPremiumCalculationStrategy implements PremiumCalculationStrategy {

    @Override
    public PolicyType getPolicyType() {
        return PolicyType.HEALTH;
    }

    @Override
    public BigDecimal calculatePremium(PremiumCalculationInput input) {
        BigDecimal premium = new BigDecimal("500.00"); // Base premium

        // Age factor
        if (input.getAge() >= 30 && input.getAge() <= 50) {
            premium = premium.add(new BigDecimal("150.00"));
        } else if (input.getAge() > 50) {
            premium = premium.add(new BigDecimal("350.00"));
        }

        // Risk factor
        BigDecimal riskCharge = new BigDecimal(String.valueOf(input.getRiskScore())).multiply(new BigDecimal("50.00"));
        premium = premium.add(riskCharge);

        // Claims factor
        BigDecimal claimsCharge = new BigDecimal(String.valueOf(input.getExistingClaims())).multiply(new BigDecimal("100.00"));
        premium = premium.add(claimsCharge);

        // Occupation factor
        if ("HIGH_RISK".equalsIgnoreCase(input.getOccupation())) {
            premium = premium.add(new BigDecimal("200.00"));
        }

        return premium;
    }
}
