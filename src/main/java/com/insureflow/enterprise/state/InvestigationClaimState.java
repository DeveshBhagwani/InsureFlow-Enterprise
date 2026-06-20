package com.insureflow.enterprise.state;

import com.insureflow.enterprise.exception.BusinessException;
import com.insureflow.enterprise.model.Claim;
import com.insureflow.enterprise.model.ClaimStatus;

public class InvestigationClaimState implements ClaimState {

    @Override
    public void review(Claim claim, String notes, String actor) {
        throw new BusinessException("Cannot review a claim that is currently under investigation.");
    }

    @Override
    public void investigate(Claim claim, String notes, String actor) {
        throw new BusinessException("Claim is already under investigation.");
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
