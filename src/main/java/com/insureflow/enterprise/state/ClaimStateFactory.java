package com.insureflow.enterprise.state;

import com.insureflow.enterprise.model.ClaimStatus;

import java.util.Map;

public class ClaimStateFactory {

    private static final Map<ClaimStatus, ClaimState> STATES = Map.of(
            ClaimStatus.SUBMITTED, new SubmittedClaimState(),
            ClaimStatus.UNDER_REVIEW, new UnderReviewClaimState(),
            ClaimStatus.INVESTIGATION, new InvestigationClaimState(),
            ClaimStatus.APPROVED, new ApprovedClaimState(),
            ClaimStatus.REJECTED, new RejectedClaimState()
    );

    public static ClaimState getState(ClaimStatus status) {
        ClaimState state = STATES.get(status);
        if (state == null) {
            throw new IllegalArgumentException("Unknown claim status: " + status);
        }
        return state;
    }
}
