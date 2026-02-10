package com.backend.branch.event;

import org.springframework.stereotype.Component;

@Component
public class NoopBranchEventPublisher implements BranchEventPublisher {
    @Override
    public void publishBranchCreated(BranchEventPayload payload) {
    }

    @Override
    public void publishBranchUpdated(BranchEventPayload payload) {
    }

    @Override
    public void publishBranchStatusChanged(BranchEventPayload payload) {
    }
}
