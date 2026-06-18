package com.insureflow.enterprise.repository;

import com.insureflow.enterprise.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUserId(Long userId);

    Optional<Customer> findByUserEmail(String email);

    boolean existsByUserId(Long userId);

    @Query("SELECT c FROM Customer c JOIN c.user u WHERE " +
           "(u.enabled = true) AND " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.phone) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Customer> searchCustomers(@Param("query") String query, Pageable pageable);
}
