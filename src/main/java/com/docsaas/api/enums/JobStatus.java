package com.docsaas.api.enums;

/** Job lifecycle (SRS 5.4). QUEUED -> PROCESSING -> SUCCEEDED | FAILED; any terminal job -> EXPIRED. */
public enum JobStatus {
    QUEUED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    EXPIRED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == EXPIRED;
    }
}
