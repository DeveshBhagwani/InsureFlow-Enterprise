package com.insureflow.enterprise.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "kyc_document_path", length = 512)
    private String kycDocumentPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 50)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Address address;

    @Builder.Default
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Nominee> nominees = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void setAddress(Address address) {
        if (address == null) {
            if (this.address != null) {
                this.address.setCustomer(null);
            }
        } else {
            address.setCustomer(this);
        }
        this.address = address;
    }

    public void addNominee(Nominee nominee) {
        nominees.add(nominee);
        nominee.setCustomer(this);
    }

    public void removeNominee(Nominee nominee) {
        nominees.remove(nominee);
        nominee.setCustomer(null);
    }
}
