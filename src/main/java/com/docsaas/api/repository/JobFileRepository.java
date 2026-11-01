package com.docsaas.api.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.docsaas.api.model.JobFile;

public interface JobFileRepository extends CrudRepository<JobFile, Long> {

    List<JobFile> findByJobIdAndRoleOrderBySortOrder(Long jobId, String role);

    List<JobFile> findByJobId(Long jobId);
}
