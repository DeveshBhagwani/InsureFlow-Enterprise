package com.insureflow.enterprise.strategy;

import com.insureflow.enterprise.model.PolicyType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LifePremiumCalculationStrategy implements PremiumCalculationStrategy {

    @Override
    public PolicyType getPolicyType() {
        return PolicyType.LIFE;
    }

    @Override
    public BigDecimal calculatePremium(PremiumCalculationInput input) {
        BigDecimal premium = new BigDecimal("400.00"); // Base premium

        // Age factor
        if (input.getAge() < 30) {
            premium = premium.add(new BigDecimal("50.00"));
        } else if (input.getAge() >= 30 && input.getAge() <= 50) {
            premium = premium.add(new BigDecimal("150.00"));
        } else {
            premium = premium.add(new BigDecimal("300.00"));
        }

        // Risk factor
        BigDecimal riskCharge = new BigDecimal(String.valueOf(input.getRiskScore())).multiply(new BigDecimal("100.00"));
        premium = premium.add(riskCharge);

        // Claims factor
        BigDecimal claimsCharge = new BigDecimal(String.valueOf(input.getExistingClaims())).multiply(new BigDecimal("200.00"));
        premium = premium.add(claimsCharge);

        // Occupation factor
        if ("HAZARDOUS".equalsIgnoreCase(input.getOccupation()) || "MILITARY".equalsIgnoreCase(input.getOccupation())) {
            premium = premium.add(new BigDecimal("250.00"));
        }

        return premium;
    }
}
