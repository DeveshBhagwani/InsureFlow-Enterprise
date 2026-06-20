package com.insureflow.enterprise.state;

import com.insureflow.enterprise.exception.BusinessException;
import com.insureflow.enterprise.model.Claim;

public class ApprovedClaimState implements ClaimState {

    private void throwTerminalStateError() {
        throw new BusinessException("Claim is already APPROVED. No further transitions allowed.");
    }

    @Override
    public void review(Claim claim, String notes, String actor) {
        throwTerminalStateError();
    }

    @Override
    public void investigate(Claim claim, String notes, String actor) {
        throwTerminalStateError();
    }

    @Override
    public void approve(Claim claim, String notes, String actor) {
        throwTerminalStateError();
    }

    @Override
    public void reject(Claim claim, String notes, String actor) {
        throwTerminalStateError();
    }
}
