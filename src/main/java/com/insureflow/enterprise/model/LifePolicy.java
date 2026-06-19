package com.insureflow.enterprise.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "life_policies")
@PrimaryKeyJoinColumn(name = "policy_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LifePolicy extends Policy {

    @Column(name = "term_years")
    private Integer termYears;

    @Column(nullable = false)
    private boolean smoker;

    @Column(name = "medical_history_summary", columnDefinition = "TEXT")
    private String medicalHistorySummary;
}
