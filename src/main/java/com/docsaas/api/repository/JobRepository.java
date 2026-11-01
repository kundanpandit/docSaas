package com.docsaas.api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import com.docsaas.api.model.Job;

public interface JobRepository extends CrudRepository<Job, Long> {

    Optional<Job> findByPublicId(String publicId);

    @Query("SELECT * FROM jobs WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    List<Job> findRecentByUserId(Long userId, int limit);

    @Query("SELECT * FROM jobs WHERE status <> 'EXPIRED' AND expires_at < :now")
    List<Job> findExpired(LocalDateTime now);

    /**
     * Safety net: jobs the engine never finished (e.g. retries exhausted while queued) are failed
     * so clients stop polling. Interrupted jobs are normally resumed by JobRunr before this triggers.
     */
    @Modifying
    @Query("UPDATE jobs SET status = 'FAILED', error_code = 'TIMED_OUT', "
            + "error_message = 'Processing took too long. Please try again.', completed_at = :now "
            + "WHERE status IN ('QUEUED', 'PROCESSING') AND created_at < :cutoff")
    int failStuck(LocalDateTime now, LocalDateTime cutoff);
}
