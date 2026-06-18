package com.insureflow.enterprise.repository;

import com.insureflow.enterprise.model.Role;
import com.insureflow.enterprise.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(UserRole name);
}
