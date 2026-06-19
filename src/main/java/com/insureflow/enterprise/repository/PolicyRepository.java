package com.insureflow.enterprise.repository;

import com.insureflow.enterprise.model.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByPolicyNumber(String policyNumber);

    Page<Policy> findByCustomerId(Long customerId, Pageable pageable);

    Page<Policy> findByCustomerUserEmail(String email, Pageable pageable);

    @Query("SELECT p FROM Policy p JOIN p.customer c JOIN c.user u WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(p.policyNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Policy> searchPolicies(@Param("query") String query, Pageable pageable);
}
