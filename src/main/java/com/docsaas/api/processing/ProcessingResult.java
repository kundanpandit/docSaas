package com.docsaas.api.processing;

import java.nio.file.Path;

/**
 * @param file     generated output inside the work directory
 * @param fileName download filename shown to the user
 * @param summary  short user-facing result description (e.g. size reduction), may be null
 */
public record ProcessingResult(Path file, String fileName, String mimeType, String summary) {
}
