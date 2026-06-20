package com.insureflow.enterprise.state;

import com.insureflow.enterprise.exception.BusinessException;
import com.insureflow.enterprise.model.Claim;
import com.insureflow.enterprise.model.ClaimStatus;

public class UnderReviewClaimState implements ClaimState {

    @Override
    public void review(Claim claim, String notes, String actor) {
        throw new BusinessException("Claim is already under review.");
    }

    @Override
    public void investigate(Claim claim, String notes, String actor) {
        claim.setStatus(ClaimStatus.INVESTIGATION);
    }

    @Override
    public void approve(Claim claim, String notes, String actor) {
        claim.setStatus(ClaimStatus.APPROVED);
    }

    @Override
    public void reject(Claim claim, String notes, String actor) {
        claim.setStatus(ClaimStatus.REJECTED);
    }
}
