package com.insureflow.enterprise.repository;

import com.insureflow.enterprise.model.PolicyHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyHistoryRepository extends JpaRepository<PolicyHistory, Long> {
    List<PolicyHistory> findByPolicyIdOrderByChangedAtDesc(Long policyId);
}
