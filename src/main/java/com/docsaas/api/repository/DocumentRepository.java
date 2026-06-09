package com.docsaas.api.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.docsaas.api.model.Document;

public interface DocumentRepository extends CrudRepository<Document, Long> {
    List<Document> findByUserId(Long userId);
}

