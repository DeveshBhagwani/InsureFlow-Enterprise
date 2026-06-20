package com.insureflow.enterprise.config;

import com.insureflow.enterprise.model.*;
import com.insureflow.enterprise.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Long entityId = null;
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Long) {
                entityId = (Long) arg;
                break;
            }
        }

        String beforeState = null;
        if (entityId != null) {
            beforeState = fetchFlatState(auditable.entityType(), entityId);
        }

        // Proceed with method execution
        Object result = joinPoint.proceed();

        // Extract ID for creations
        if (entityId == null && result != null) {
            entityId = extractIdFromResponse(result);
        }

        String afterState = null;
        if (entityId != null) {
            afterState = fetchFlatState(auditable.entityType(), entityId);
        }

        String currentUserEmail = "system";
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            currentUserEmail = auth.getName();
        }

        AuditLog logEntry = AuditLog.builder()
                .userEmail(currentUserEmail)
                .action(auditable.action())
                .entityName(auditable.entityType())
                .entityId(entityId != null ? entityId : 0L)
                .beforeState(beforeState)
                .afterState(afterState)
                .build();

        auditLogRepository.save(logEntry);
        log.debug("Logged audit action: {} on {} ID {}", auditable.action(), auditable.entityType(), entityId);

        return result;
    }

    private String fetchFlatState(String entityType, Long id) {
        try {
            if (id == null) return null;
            Object entity = null;
            if ("Customer".equalsIgnoreCase(entityType)) {
                entity = entityManager.find(Customer.class, id);
            } else if ("Policy".equalsIgnoreCase(entityType)) {
                entity = entityManager.find(Policy.class, id);
            } else if ("Claim".equalsIgnoreCase(entityType)) {
                entity = entityManager.find(Claim.class, id);
            } else if ("Payment".equalsIgnoreCase(entityType)) {
                entity = entityManager.find(Payment.class, id);
            }

            if (entity == null) return null;
            return buildFlatJson(entity);
        } catch (Exception e) {
            log.warn("Failed to capture audit log state for {} with ID {}", entityType, id, e);
            return null;
        }
    }

    private String buildFlatJson(Object entity) {
        if (entity instanceof Customer c) {
            return String.format("{\"id\":%d,\"phone\":\"%s\",\"kycStatus\":\"%s\",\"deleted\":%b}",
                    c.getId(), c.getPhone(), c.getKycStatus() != null ? c.getKycStatus().name() : null, c.isDeleted());
        } else if (entity instanceof Policy p) {
            return String.format("{\"id\":%d,\"policyNumber\":\"%s\",\"status\":\"%s\",\"premiumAmount\":%s,\"coverageAmount\":%s}",
                    p.getId(), p.getPolicyNumber(), p.getStatus().name(), p.getPremiumAmount(), p.getCoverageAmount());
        } else if (entity instanceof Claim c) {
            return String.format("{\"id\":%d,\"claimNumber\":\"%s\",\"status\":\"%s\",\"claimAmount\":%s}",
                    c.getId(), c.getClaimNumber(), c.getStatus().name(), c.getClaimAmount());
        } else if (entity instanceof Payment p) {
            return String.format("{\"id\":%d,\"paymentNumber\":\"%s\",\"status\":\"%s\",\"amount\":%s,\"transactionType\":\"%s\"}",
                    p.getId(), p.getPaymentNumber(), p.getStatus().name(), p.getAmount(), p.getTransactionType().name());
        }
        return null;
    }

    private Long extractIdFromResponse(Object result) {
        try {
            if (result == null) return null;
            
            // If the result is the DTO response directly, or wrapped in another response type
            if (result.getClass().getName().contains("ApiResponse")) {
                java.lang.reflect.Method getDataMethod = result.getClass().getMethod("getData");
                result = getDataMethod.invoke(result);
            }
            
            if (result == null) return null;
            
            java.lang.reflect.Method getIdMethod = result.getClass().getMethod("getId");
            Object idVal = getIdMethod.invoke(result);
            if (idVal instanceof Long) {
                return (Long) idVal;
            }
        } catch (Exception e) {
            log.warn("Failed to extract ID from result object for auditing", e);
        }
        return null;
    }
}
