package com.insureflow.enterprise.repository;

import com.insureflow.enterprise.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p WHERE p.policy.customer.user.email = :email")
    Page<Payment> findByCustomerUserEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT p FROM Payment p JOIN p.policy pol JOIN pol.customer cust JOIN cust.user u WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(p.paymentNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(pol.policyNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Payment> searchPayments(@Param("query") String query, Pageable pageable);
}
