package com.insureflow.enterprise.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "health_policies")
@PrimaryKeyJoinColumn(name = "policy_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HealthPolicy extends Policy {

    @Column(precision = 12, scale = 2)
    private BigDecimal deductible;

    @Column(name = "co_pay_percentage", precision = 5, scale = 2)
    private BigDecimal coPayPercentage;

    @Column(name = "pre_existing_conditions", columnDefinition = "TEXT")
    private String preExistingConditions;
}
