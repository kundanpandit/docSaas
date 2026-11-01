package com.docsaas.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Daily credits and recent ledger entries of the logged-in user.
 *
 * @param balance       credits left today
 * @param dailyCredits  credits granted every day
 * @param usedToday     credits spent today on jobs that didn't fail
 * @param usedThisMonth credits spent on jobs since the 1st of this month, net of refunds
 * @param resetsAt      when the daily allowance resets
 * @param enforced      whether jobs are blocked when the balance is too low
 * @param costs         credits per job, by tool id
 */
public record CreditsDTO(int balance, int dailyCredits, int usedToday, int usedThisMonth, LocalDateTime resetsAt,
        boolean enforced, Map<String, Integer> costs, List<Entry> recent) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Entry(int delta, String type, String tool, String jobId, String reason, LocalDateTime createdAt) {
    }
}
