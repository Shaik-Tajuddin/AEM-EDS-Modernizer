package com.adobe.aem.modernizer.agents;

/**
 * Migration State Machine States and Transition Guards (Master §5, ADR 0013).
 */
public enum MigrationState {
    CREATED,
    CONNECTING,
    DISCOVERING,
    ANALYZING,
    DESIGN_ANALYSIS,
    PLANNING,
    BUILDING,
    MIGRATING,
    AUTHORING,
    PREVIEWING,
    VALIDATING,
    REPAIRING,
    READY_TO_PUBLISH,
    PUBLISHING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED,
    WAITING_FOR_CLARIFICATION;

    public boolean canTransitionTo(MigrationState next) {
        if (next == null) return false;
        if (next == FAILED || next == CANCELLED) return true; // Can always fail or be cancelled

        switch (this) {
            case CREATED:
                return next == CONNECTING;
            case CONNECTING:
                return next == DISCOVERING;
            case DISCOVERING:
                return next == ANALYZING;
            case ANALYZING:
                return next == DESIGN_ANALYSIS;
            case DESIGN_ANALYSIS:
                return next == PLANNING;
            case PLANNING:
                // If dry run, finishes at READY_TO_PUBLISH or COMPLETED
                return next == BUILDING || next == READY_TO_PUBLISH || next == COMPLETED || next == WAITING_FOR_CLARIFICATION;
            case BUILDING:
                return next == MIGRATING;
            case MIGRATING:
                return next == AUTHORING;
            case AUTHORING:
                return next == PREVIEWING;
            case PREVIEWING:
                return next == VALIDATING;
            case VALIDATING:
                return next == REPAIRING || next == READY_TO_PUBLISH || next == WAITING_FOR_CLARIFICATION;
            case REPAIRING:
                return next == VALIDATING || next == READY_TO_PUBLISH || next == FAILED;
            case READY_TO_PUBLISH:
                return next == PUBLISHING || next == COMPLETED;
            case PUBLISHING:
                return next == VERIFYING;
            case VERIFYING:
                return next == COMPLETED;
            case WAITING_FOR_CLARIFICATION:
                return next == PLANNING || next == VALIDATING || next == BUILDING;
            case COMPLETED:
            case FAILED:
            case CANCELLED:
            default:
                return false;
        }
    }
}
