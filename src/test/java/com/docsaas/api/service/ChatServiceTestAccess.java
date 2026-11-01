package com.docsaas.api.service;

import java.util.List;

import com.docsaas.api.processing.engine.DocumentIndex;

/** Exposes package-private ChatService helpers to tests in other packages. */
public final class ChatServiceTestAccess {

    private ChatServiceTestAccess() {
    }

    public static List<Integer> citedPages(String answer, List<DocumentIndex.Chunk> excerpts) {
        return ChatService.citedPages(answer, excerpts);
    }
}
