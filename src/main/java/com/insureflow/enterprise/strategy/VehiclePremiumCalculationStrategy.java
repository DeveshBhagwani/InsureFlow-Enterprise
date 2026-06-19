package com.insureflow.enterprise.strategy;

import com.insureflow.enterprise.model.PolicyType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class VehiclePremiumCalculationStrategy implements PremiumCalculationStrategy {

    @Override
    public PolicyType getPolicyType() {
        return PolicyType.VEHICLE;
    }

    @Override
    public BigDecimal calculatePremium(PremiumCalculationInput input) {
        BigDecimal premium = new BigDecimal("300.00"); // Base premium

        // Age factor (young drivers and seniors have higher risk)
        if (input.getAge() < 25) {
            premium = premium.add(new BigDecimal("250.00"));
        } else if (input.getAge() >= 25 && input.getAge() <= 60) {
            premium = premium.add(new BigDecimal("50.00"));
        } else {
            premium = premium.add(new BigDecimal("150.00"));
        }

        // Risk factor
        BigDecimal riskCharge = new BigDecimal(String.valueOf(input.getRiskScore())).multiply(new BigDecimal("80.00"));
        premium = premium.add(riskCharge);

        // Claims factor
        BigDecimal claimsCharge = new BigDecimal(String.valueOf(input.getExistingClaims())).multiply(new BigDecimal("150.00"));
        premium = premium.add(claimsCharge);

        // Occupation factor
        if ("DRIVER".equalsIgnoreCase(input.getOccupation()) || "DELIVERY".equalsIgnoreCase(input.getOccupation())) {
            premium = premium.add(new BigDecimal("150.00"));
        }

        return premium;
    }
}
