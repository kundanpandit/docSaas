package com.docsaas.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.docsaas.api.dto.CreditsDTO;
import com.docsaas.api.dto.UsageDTO;
import com.docsaas.api.enums.ToolCode;
import com.docsaas.api.exception.ApiException;
import com.docsaas.api.model.Job;

/**
 * Server-authoritative daily credits (FR-023, FR-024).
 *
 * Policy: every visitor gets a daily allowance that resets at midnight (server time).
 * Guests get {@code docsaas.credits.guest-daily} credits and cannot use AI tools; logged-in users
 * get {@code docsaas.credits.user-daily} credits and every tool. A job costs its tool's price when it
 * is accepted; failed jobs don't count, so a failure is refunded automatically.
 *
 * Usage is computed from the jobs table (guests by IP, users by id), so there is one source of
 * truth. Registered users additionally get an append-only ledger with idempotent entries for audit
 * and the history shown on the Credits page.
 */
@Service
public class CreditService {

    private static final Logger log = LoggerFactory.getLogger(CreditService.class);

    public static final String JOB_CHARGE = "JOB_CHARGE";
    public static final String JOB_REFUND = "JOB_REFUND";

    public static final Set<ToolCode> AI_TOOLS = EnumSet.of(ToolCode.AI_SUMMARY, ToolCode.DOCUMENT_INSIGHTS,
            ToolCode.CHAT_WITH_PDF);

    /** Failed jobs, including failed jobs that have since expired (they keep their error code). */
    private static final String FAILED_JOB = "(j.status = 'FAILED' OR (j.status = 'EXPIRED' AND j.error_code IS NOT NULL))";

    private final NamedParameterJdbcTemplate jdbc;

    /** Serialises check-and-create per visitor so two parallel uploads can't both spend the last credit. */
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    @Value("${docsaas.credits.enforce:true}")
    private boolean enforce;

    @Value("${docsaas.credits.guest-daily:3}")
    private int guestDaily;

    @Value("${docsaas.credits.user-daily:30}")
    private int userDaily;

    @Value("${docsaas.credits.cost.standard:1}")
    private int standardCost;

    @Value("${docsaas.credits.cost.ai:3}")
    private int aiCost;

    public CreditService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int costOf(ToolCode tool) {
        return AI_TOOLS.contains(tool) ? aiCost : standardCost;
    }

    public static boolean isAi(ToolCode tool) {
        return AI_TOOLS.contains(tool);
    }

    /** Lock object for one visitor; callers hold it from the affordability check until the job is saved. */
    public Object lockFor(Long userId, String clientIp) {
        return locks.computeIfAbsent(userId != null ? "u:" + userId : "ip:" + clientIp, k -> new Object());
    }

    /**
     * Rejects the job up front: AI tools need an account, and the day's remaining credits must cover
     * the tool. Must be called while holding {@link #lockFor}.
     */
    public void checkAllowed(Long userId, String clientIp, ToolCode tool) {
        if (userId == null && isAi(tool)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED",
                    "AI tools are available after you log in. It's free, and you get " + userDaily
                            + " credits a day.");
        }
        if (!enforce) {
            return;
        }
        int cost = costOf(tool);
        int remaining = dailyAllowance(userId) - usedToday(userId, clientIp);
        if (remaining < cost) {
            String needs = cost == 1 ? "1 credit" : cost + " credits";
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_CREDITS", userId == null
                    ? "You've used today's " + guestDaily + " free guest credits. Log in free to get " + userDaily
                            + " credits a day and unlock AI tools."
                    : "You've used today's credits (" + needs + " needed, " + Math.max(remaining, 0)
                            + " left). Your credits reset at midnight.");
        }
    }

    public int dailyAllowance(Long userId) {
        return userId != null ? userDaily : guestDaily;
    }

    /** Credits spent since midnight on jobs that didn't fail. Guests are counted by IP. */
    public int usedToday(Long userId, String clientIp) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("since", LocalDate.now().atStartOfDay())
                .addValue("ai", AI_TOOLS.stream().map(Enum::name).toList())
                .addValue("aiCost", aiCost).addValue("std", standardCost);
        String who;
        if (userId != null) {
            who = "j.user_id = :u";
            p.addValue("u", userId);
        } else {
            who = "j.user_id IS NULL AND j.client_ip = :ip";
            p.addValue("ip", clientIp == null ? "" : clientIp);
        }
        Integer used = jdbc.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN j.tool_code IN (:ai) THEN :aiCost ELSE :std END), 0)
                FROM jobs j WHERE """ + " " + who + " AND j.created_at >= :since AND NOT " + FAILED_JOB, p,
                Integer.class);
        return used == null ? 0 : used;
    }

    /** Public usage summary for the usage widget: works for guests and logged-in users. */
    public UsageDTO usage(Long userId, String clientIp) {
        int allowance = dailyAllowance(userId);
        int used = usedToday(userId, clientIp);
        Integer jobsToday = jdbc.queryForObject(
                "SELECT COUNT(*) FROM jobs j WHERE " + (userId != null ? "j.user_id = :u" : "j.user_id IS NULL AND j.client_ip = :ip")
                        + " AND j.created_at >= :since AND NOT " + FAILED_JOB,
                new MapSqlParameterSource().addValue("u", userId).addValue("ip", clientIp == null ? "" : clientIp)
                        .addValue("since", LocalDate.now().atStartOfDay()),
                Integer.class);
        return new UsageDTO(userId != null, allowance, used, Math.max(allowance - used, 0),
                jobsToday == null ? 0 : jobsToday, LocalDate.now().plusDays(1).atStartOfDay(), userId == null,
                guestDaily, userDaily, standardCost, aiCost, enforce);
    }

    /** Records the charge of an accepted job of a registered user in the ledger (audit/history). */
    public void chargeJob(Job job) {
        if (job.getUserId() == null) {
            return;
        }
        insert(job.getUserId(), -costOf(job.getToolCode()), JOB_CHARGE, job.getId(), "charge:" + job.getId(),
                job.getToolCode().getSlug());
    }

    /** Records the refund of one failed job, if it was charged and not refunded yet. */
    public void refundIfFailed(Long jobId) {
        int n = jdbc.update(refundSql("AND j.id = :jobId"), Map.of("jobId", jobId, "now", LocalDateTime.now()));
        if (n > 0) {
            log.info("Refunded credits for failed job id={}", jobId);
        }
    }

    /** Sweep: refunds every failed job that was charged and not refunded (e.g. jobs failed by the stuck-job sweeper). */
    public int refundAllFailed() {
        return jdbc.update(refundSql(""), Map.of("now", LocalDateTime.now()));
    }

    public CreditsDTO summary(Long userId) {
        UsageDTO usage = usage(userId, null);
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Integer usedMonth = jdbc.queryForObject("""
                SELECT COALESCE(-SUM(delta), 0) FROM credit_ledger
                WHERE user_id = :u AND event_type IN ('JOB_CHARGE', 'JOB_REFUND') AND created_at >= :since""",
                Map.of("u", userId, "since", monthStart), Integer.class);

        List<CreditsDTO.Entry> recent = jdbc.query("""
                SELECT l.delta, l.event_type, l.reason, l.created_at, j.public_id, j.tool_code
                FROM credit_ledger l LEFT JOIN jobs j ON j.id = l.related_job_id
                WHERE l.user_id = :u AND l.event_type IN ('JOB_CHARGE', 'JOB_REFUND')
                ORDER BY l.created_at DESC, l.id DESC LIMIT 30""",
                Map.of("u", userId),
                (rs, i) -> new CreditsDTO.Entry(rs.getInt("delta"), rs.getString("event_type"),
                        toolSlug(rs.getString("tool_code")), rs.getString("public_id"), rs.getString("reason"),
                        rs.getTimestamp("created_at").toLocalDateTime()));

        Map<String, Integer> costs = new LinkedHashMap<>();
        for (ToolCode tool : ToolCode.values()) {
            costs.put(tool.getSlug(), costOf(tool));
        }
        return new CreditsDTO(usage.remaining(), usage.dailyCredits(), usage.used(),
                usedMonth == null ? 0 : usedMonth, usage.resetsAt(), enforce, costs, recent);
    }

    /** Removes all ledger entries of a deleted account. */
    public void deleteAll(Long userId) {
        jdbc.update("DELETE FROM credit_ledger WHERE user_id = :u", Map.of("u", userId));
    }

    // ===== helpers =====

    private void insert(Long userId, int delta, String type, Long jobId, String key, String reason) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("u", userId).addValue("d", delta).addValue("t", type).addValue("j", jobId)
                .addValue("k", key).addValue("r", reason).addValue("now", LocalDateTime.now());
        // INSERT IGNORE: the unique idempotency key turns repeats into no-ops
        jdbc.update("""
                INSERT IGNORE INTO credit_ledger (user_id, delta, event_type, related_job_id, idempotency_key, reason, created_at)
                VALUES (:u, :d, :t, :j, :k, :r, :now)""", p);
    }

    private static String refundSql(String filter) {
        return """
                INSERT IGNORE INTO credit_ledger (user_id, delta, event_type, related_job_id, idempotency_key, reason, created_at)
                SELECT c.user_id, -c.delta, 'JOB_REFUND', c.related_job_id, CONCAT('refund:', c.related_job_id), c.reason, :now
                FROM credit_ledger c JOIN jobs j ON j.id = c.related_job_id
                WHERE c.event_type = 'JOB_CHARGE' AND """ + FAILED_JOB + " " + filter;
    }

    private static String toolSlug(String toolCode) {
        if (toolCode == null) {
            return null;
        }
        try {
            return ToolCode.valueOf(toolCode).getSlug();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
