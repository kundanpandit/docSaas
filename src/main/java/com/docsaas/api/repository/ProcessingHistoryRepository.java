package com.docsaas.api.repository;

import com.docsaas.api.model.ProcessingHistory;
import org.springframework.data.repository.CrudRepository;

public interface ProcessingHistoryRepository
        extends CrudRepository<ProcessingHistory, Long> {
}
