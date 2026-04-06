package com.backend.branch.event;

public interface BranchEventPublisher {
    void publishBranchCreated(BranchEventPayload payload);

    void publishBranchUpdated(BranchEventPayload payload);

    void publishBranchStatusChanged(BranchEventPayload payload);
}
