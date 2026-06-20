package com.insureflow.enterprise.repository;

import com.insureflow.enterprise.model.Claim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    @Query("SELECT c FROM Claim c WHERE c.policy.customer.user.email = :email")
    Page<Claim> findByCustomerUserEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT c FROM Claim c JOIN c.policy p JOIN p.customer cust JOIN cust.user u WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(c.claimNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.policyNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Claim> searchClaims(@Param("query") String query, Pageable pageable);
}
