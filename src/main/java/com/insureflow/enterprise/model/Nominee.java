package com.insureflow.enterprise.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "nominees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nominee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 100)
    private String relationship;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(length = 20)
    private String phone;
}
