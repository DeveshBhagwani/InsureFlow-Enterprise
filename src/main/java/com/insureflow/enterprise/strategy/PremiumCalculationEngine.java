package com.insureflow.enterprise.strategy;

import com.insureflow.enterprise.exception.BusinessException;
import com.insureflow.enterprise.model.PolicyType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PremiumCalculationEngine {

    private final Map<PolicyType, PremiumCalculationStrategy> strategies;

    public PremiumCalculationEngine(List<PremiumCalculationStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(PremiumCalculationStrategy::getPolicyType, Function.identity()));
    }

    public BigDecimal calculate(PremiumCalculationInput input) {
        if (input == null || input.getPolicyType() == null) {
            throw new BusinessException("Policy type is required for premium calculation.");
        }

        PremiumCalculationStrategy strategy = strategies.get(input.getPolicyType());
        if (strategy == null) {
            throw new BusinessException("No premium calculation strategy found for policy type: " + input.getPolicyType());
        }

        return strategy.calculatePremium(input);
    }
}
