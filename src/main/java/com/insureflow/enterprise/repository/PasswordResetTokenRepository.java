package com.insureflow.enterprise.repository;

import com.insureflow.enterprise.model.PasswordResetToken;
import com.insureflow.enterprise.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    
    @Modifying
    int deleteByUser(User user);
}
