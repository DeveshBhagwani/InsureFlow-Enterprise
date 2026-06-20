package com.insureflow.enterprise.model;

import com.insureflow.enterprise.state.ClaimStateFactory;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "claims")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"policy", "history"})
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_number", nullable = false, unique = true, length = 50)
    private String claimNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "claim_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal claimAmount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClaimStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "claim_documents", joinColumns = @JoinColumn(name = "claim_id"))
    @Column(name = "document_path")
    @Builder.Default
    private List<String> documentPaths = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClaimHistory> history = new ArrayList<>();

    public void addDocumentPath(String path) {
        if (this.documentPaths == null) {
            this.documentPaths = new ArrayList<>();
        }
        this.documentPaths.add(path);
    }

    public void addHistory(ClaimHistory historyEntry) {
        if (this.history == null) {
            this.history = new ArrayList<>();
        }
        this.history.add(historyEntry);
        historyEntry.setClaim(this);
    }

    // State Pattern delegation
    public void review(String notes, String actor) {
        ClaimStateFactory.getState(this.status).review(this, notes, actor);
    }

    public void investigate(String notes, String actor) {
        ClaimStateFactory.getState(this.status).investigate(this, notes, actor);
    }

    public void approve(String notes, String actor) {
        ClaimStateFactory.getState(this.status).approve(this, notes, actor);
    }

    public void reject(String notes, String actor) {
        ClaimStateFactory.getState(this.status).reject(this, notes, actor);
    }
}
