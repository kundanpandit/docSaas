package com.docsaas.api.dto;

import java.time.LocalDateTime;

/**
 * Today's credit usage of the current visitor (guest or logged-in), for the usage widget.
 *
 * @param dailyCredits credits this visitor gets per day
 * @param used         credits spent today on jobs that didn't fail
 * @param remaining    credits left today
 * @param jobsToday    jobs run today that didn't fail
 * @param resetsAt     when the allowance resets (next midnight, server time)
 * @param aiLocked     true when AI tools need a login
 * @param guestDaily   guest allowance, for "log in to get more" messaging
 * @param userDaily    logged-in allowance
 */
public record UsageDTO(boolean loggedIn, int dailyCredits, int used, int remaining, int jobsToday,
        LocalDateTime resetsAt, boolean aiLocked, int guestDaily, int userDaily, int standardCost, int aiCost,
        boolean enforced) {
}
