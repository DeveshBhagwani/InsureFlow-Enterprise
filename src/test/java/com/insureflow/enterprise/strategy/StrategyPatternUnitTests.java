package com.insureflow.enterprise.strategy;

import com.insureflow.enterprise.exception.BusinessException;
import com.insureflow.enterprise.model.PolicyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class StrategyPatternUnitTests {

    private PremiumCalculationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PremiumCalculationEngine(Arrays.asList(
                new HealthPremiumCalculationStrategy(),
                new VehiclePremiumCalculationStrategy(),
                new LifePremiumCalculationStrategy()
        ));
    }

    @Test
    void testEngineValidationExceptions() {
        assertThrows(BusinessException.class, () -> engine.calculate(null));
        assertThrows(BusinessException.class, () -> engine.calculate(PremiumCalculationInput.builder().build()));

        // Test strategy not found
        PremiumCalculationEngine missingStrategyEngine = new PremiumCalculationEngine(Arrays.asList(
                new HealthPremiumCalculationStrategy()
        ));
        PremiumCalculationInput lifeInput = PremiumCalculationInput.builder()
                .policyType(PolicyType.LIFE)
                .build();
        assertThrows(BusinessException.class, () -> missingStrategyEngine.calculate(lifeInput));
    }

    @Test
    void testHealthStrategy_DifferentAges() {
        // Base = 500, Occupation = "", Risk = 0, Claims = 0

        // Age < 30
        PremiumCalculationInput input1 = PremiumCalculationInput.builder()
                .policyType(PolicyType.HEALTH)
                .age(25)
                .riskScore(0)
                .existingClaims(0)
                .occupation("OFFICE")
                .build();
        assertEquals(new BigDecimal("500.00"), engine.calculate(input1));

        // Age between 30 and 50 (+150)
        PremiumCalculationInput input2 = PremiumCalculationInput.builder()
                .policyType(PolicyType.HEALTH)
                .age(35)
                .riskScore(0)
                .existingClaims(0)
                .occupation("OFFICE")
                .build();
        assertEquals(new BigDecimal("650.00"), engine.calculate(input2));

        // Age > 50 (+350)
        PremiumCalculationInput input3 = PremiumCalculationInput.builder()
                .policyType(PolicyType.HEALTH)
                .age(60)
                .riskScore(0)
                .existingClaims(0)
                .occupation("OFFICE")
                .build();
        assertEquals(new BigDecimal("850.00"), engine.calculate(input3));
    }

    @Test
    void testHealthStrategy_RiskClaimsOccupation() {
        // Age = 25 (Base 500)
        // Risk = 3 (+150)
        // Claims = 2 (+200)
        // Occupation = HIGH_RISK (+200)
        PremiumCalculationInput input = PremiumCalculationInput.builder()
                .policyType(PolicyType.HEALTH)
                .age(25)
                .riskScore(3)
                .existingClaims(2)
                .occupation("HIGH_RISK")
                .build();
        assertEquals(new BigDecimal("1050.00"), engine.calculate(input));
    }

    @Test
    void testLifeStrategy_DifferentAges() {
        // Base = 400

        // Age < 30 (+50)
        PremiumCalculationInput input1 = PremiumCalculationInput.builder()
                .policyType(PolicyType.LIFE)
                .age(25)
                .riskScore(0)
                .existingClaims(0)
                .occupation("OFFICE")
                .build();
        assertEquals(new BigDecimal("450.00"), engine.calculate(input1));

        // Age between 30 and 50 (+150)
        PremiumCalculationInput input2 = PremiumCalculationInput.builder()
                .policyType(PolicyType.LIFE)
                .age(40)
                .riskScore(0)
                .existingClaims(0)
                .occupation("OFFICE")
                .build();
        assertEquals(new BigDecimal("550.00"), engine.calculate(input2));

        // Age > 50 (+300)
        PremiumCalculationInput input3 = PremiumCalculationInput.builder()
                .policyType(PolicyType.LIFE)
                .age(55)
                .riskScore(0)
                .existingClaims(0)
                .occupation("OFFICE")
                .build();
        assertEquals(new BigDecimal("700.00"), engine.calculate(input3));
    }

    @Test
    void testLifeStrategy_RiskClaimsOccupation() {
        // Age = 25 (Base 400 + 50 = 450)
        // Risk = 2 (+200)
        // Claims = 1 (+200)
        // Occupation = HAZARDOUS (+250)
        PremiumCalculationInput inputHaz = PremiumCalculationInput.builder()
                .policyType(PolicyType.LIFE)
                .age(25)
                .riskScore(2)
                .existingClaims(1)
                .occupation("HAZARDOUS")
                .build();
        assertEquals(new BigDecimal("1100.00"), engine.calculate(inputHaz));

        // Occupation = MILITARY (+250)
        PremiumCalculationInput inputMil = PremiumCalculationInput.builder()
                .policyType(PolicyType.LIFE)
                .age(25)
                .riskScore(2)
                .existingClaims(1)
                .occupation("MILITARY")
                .build();
        assertEquals(new BigDecimal("1100.00"), engine.calculate(inputMil));
    }

    @Test
    void testVehicleStrategy_DifferentAges() {
        // Base = 300

        // Age < 25 (+250)
        PremiumCalculationInput input1 = PremiumCalculationInput.builder()
                .policyType(PolicyType.VEHICLE)
                .age(20)
                .riskScore(0)
                .existingClaims(0)
                .occupation("OFFICE")
                .build();
        assertEquals(new BigDecimal("550.00"), engine.calculate(input1));

        // Age between 25 and 60 (+50)
        PremiumCalculationInput input2 = PremiumCalculationInput.builder()
                .policyType(PolicyType.VEHICLE)
                .age(35)
                .riskScore(0)
                .existingClaims(0)
                .occupation("OFFICE")
                .build();
        assertEquals(new BigDecimal("350.00"), engine.calculate(input2));

        // Age > 60 (+150)
        PremiumCalculationInput input3 = PremiumCalculationInput.builder()
                .policyType(PolicyType.VEHICLE)
                .age(65)
                .riskScore(0)
                .existingClaims(0)
                .occupation("OFFICE")
                .build();
        assertEquals(new BigDecimal("450.00"), engine.calculate(input3));
    }

    @Test
    void testVehicleStrategy_RiskClaimsOccupation() {
        // Age = 35 (Base 300 + 50 = 350)
        // Risk = 2 (+160)
        // Claims = 1 (+150)
        // Occupation = DRIVER (+150)
        PremiumCalculationInput inputDriver = PremiumCalculationInput.builder()
                .policyType(PolicyType.VEHICLE)
                .age(35)
                .riskScore(2)
                .existingClaims(1)
                .occupation("DRIVER")
                .build();
        assertEquals(new BigDecimal("810.00"), engine.calculate(inputDriver));

        // Occupation = DELIVERY (+150)
        PremiumCalculationInput inputDelivery = PremiumCalculationInput.builder()
                .policyType(PolicyType.VEHICLE)
                .age(35)
                .riskScore(2)
                .existingClaims(1)
                .occupation("DELIVERY")
                .build();
        assertEquals(new BigDecimal("810.00"), engine.calculate(inputDelivery));
    }
}
