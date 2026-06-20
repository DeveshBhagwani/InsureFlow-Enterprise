package com.insureflow.enterprise.repository;

import com.insureflow.enterprise.model.ClaimHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimHistoryRepository extends JpaRepository<ClaimHistory, Long> {
    List<ClaimHistory> findByClaimIdOrderByChangedAtDesc(Long claimId);
}
