package com.insureflow.enterprise.strategy;

import com.insureflow.enterprise.model.PolicyType;

import java.math.BigDecimal;

public interface PremiumCalculationStrategy {
    PolicyType getPolicyType();
    BigDecimal calculatePremium(PremiumCalculationInput input);
}
