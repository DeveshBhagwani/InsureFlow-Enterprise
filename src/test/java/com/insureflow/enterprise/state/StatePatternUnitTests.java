package com.insureflow.enterprise.state;

import com.insureflow.enterprise.exception.BusinessException;
import com.insureflow.enterprise.model.Claim;
import com.insureflow.enterprise.model.ClaimStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StatePatternUnitTests {

    private Claim claim;

    @BeforeEach
    void setUp() {
        claim = new Claim();
    }

    @Test
    void testClaimStateFactory() {
        assertTrue(ClaimStateFactory.getState(ClaimStatus.SUBMITTED) instanceof SubmittedClaimState);
        assertTrue(ClaimStateFactory.getState(ClaimStatus.UNDER_REVIEW) instanceof UnderReviewClaimState);
        assertTrue(ClaimStateFactory.getState(ClaimStatus.INVESTIGATION) instanceof InvestigationClaimState);
        assertTrue(ClaimStateFactory.getState(ClaimStatus.APPROVED) instanceof ApprovedClaimState);
        assertTrue(ClaimStateFactory.getState(ClaimStatus.REJECTED) instanceof RejectedClaimState);
    }

    @Test
    void testSubmittedClaimState() {
        ClaimState state = new SubmittedClaimState();
        claim.setStatus(ClaimStatus.SUBMITTED);

        // Allowed transition
        state.review(claim, "review notes", "actor");
        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());

        // Disallowed transitions
        assertThrows(BusinessException.class, () -> state.investigate(claim, "notes", "actor"));
        assertThrows(BusinessException.class, () -> state.approve(claim, "notes", "actor"));
        assertThrows(BusinessException.class, () -> state.reject(claim, "notes", "actor"));
    }

    @Test
    void testUnderReviewClaimState() {
        ClaimState state = new UnderReviewClaimState();

        // Disallowed transition
        assertThrows(BusinessException.class, () -> state.review(claim, "notes", "actor"));

        // Allowed transition to investigate
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        state.investigate(claim, "notes", "actor");
        assertEquals(ClaimStatus.INVESTIGATION, claim.getStatus());

        // Allowed transition to approve
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        state.approve(claim, "notes", "actor");
        assertEquals(ClaimStatus.APPROVED, claim.getStatus());

        // Allowed transition to reject
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        state.reject(claim, "notes", "actor");
        assertEquals(ClaimStatus.REJECTED, claim.getStatus());
    }

    @Test
    void testInvestigationClaimState() {
        ClaimState state = new InvestigationClaimState();

        // Disallowed transitions
        assertThrows(BusinessException.class, () -> state.review(claim, "notes", "actor"));
        assertThrows(BusinessException.class, () -> state.investigate(claim, "notes", "actor"));

        // Allowed transition to approve
        claim.setStatus(ClaimStatus.INVESTIGATION);
        state.approve(claim, "notes", "actor");
        assertEquals(ClaimStatus.APPROVED, claim.getStatus());

        // Allowed transition to reject
        claim.setStatus(ClaimStatus.INVESTIGATION);
        state.reject(claim, "notes", "actor");
        assertEquals(ClaimStatus.REJECTED, claim.getStatus());
    }

    @Test
    void testApprovedClaimState() {
        ClaimState state = new ApprovedClaimState();
        claim.setStatus(ClaimStatus.APPROVED);

        // All transitions terminal/disallowed
        assertThrows(BusinessException.class, () -> state.review(claim, "notes", "actor"));
        assertThrows(BusinessException.class, () -> state.investigate(claim, "notes", "actor"));
        assertThrows(BusinessException.class, () -> state.approve(claim, "notes", "actor"));
        assertThrows(BusinessException.class, () -> state.reject(claim, "notes", "actor"));
    }

    @Test
    void testRejectedClaimState() {
        ClaimState state = new RejectedClaimState();
        claim.setStatus(ClaimStatus.REJECTED);

        // All transitions terminal/disallowed
        assertThrows(BusinessException.class, () -> state.review(claim, "notes", "actor"));
        assertThrows(BusinessException.class, () -> state.investigate(claim, "notes", "actor"));
        assertThrows(BusinessException.class, () -> state.approve(claim, "notes", "actor"));
        assertThrows(BusinessException.class, () -> state.reject(claim, "notes", "actor"));
    }
}
