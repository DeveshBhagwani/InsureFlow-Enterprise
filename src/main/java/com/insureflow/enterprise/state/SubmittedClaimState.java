package com.insureflow.enterprise.state;

import com.insureflow.enterprise.exception.BusinessException;
import com.insureflow.enterprise.model.Claim;
import com.insureflow.enterprise.model.ClaimStatus;

public class SubmittedClaimState implements ClaimState {

    @Override
    public void review(Claim claim, String notes, String actor) {
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
    }

    @Override
    public void investigate(Claim claim, String notes, String actor) {
        throw new BusinessException("Cannot investigate a claim that is in SUBMITTED state. It must be reviewed first.");
    }

    @Override
    public void approve(Claim claim, String notes, String actor) {
        throw new BusinessException("Cannot approve a claim that is in SUBMITTED state. It must be reviewed first.");
    }

    @Override
    public void reject(Claim claim, String notes, String actor) {
        throw new BusinessException("Cannot reject a claim that is in SUBMITTED state. It must be reviewed first.");
    }
}
