package com.insureflow.enterprise.state;

import com.insureflow.enterprise.model.Claim;

public interface ClaimState {
    void review(Claim claim, String notes, String actor);
    void investigate(Claim claim, String notes, String actor);
    void approve(Claim claim, String notes, String actor);
    void reject(Claim claim, String notes, String actor);
}
