package com.insureflow.enterprise.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "vehicle_policies")
@PrimaryKeyJoinColumn(name = "policy_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class VehiclePolicy extends Policy {

    @Column(name = "vehicle_make", length = 100)
    private String vehicleMake;

    @Column(name = "vehicle_model", length = 100)
    private String vehicleModel;

    @Column(name = "license_plate", length = 50)
    private String licensePlate;

    @Column(name = "vehicle_value", precision = 12, scale = 2)
    private BigDecimal vehicleValue;
}
