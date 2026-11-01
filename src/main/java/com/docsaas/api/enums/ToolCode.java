package com.docsaas.api.enums;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Tools that are implemented end to end. The slug matches the frontend tool id
 * and the URL: POST /api/v1/tools/{slug}.
 */
public enum ToolCode {
    MERGE_PDF("merge-pdf", EnumSet.of(FileKind.PDF), 2, 20),
    SPLIT_PDF("split-pdf", EnumSet.of(FileKind.PDF), 1, 1),
    COMPRESS_PDF("compress-pdf", EnumSet.of(FileKind.PDF), 1, 1),
    ROTATE_PDF("rotate-pdf", EnumSet.of(FileKind.PDF), 1, 1),
    WATERMARK_PDF("watermark-pdf", EnumSet.of(FileKind.PDF), 1, 1),
    PAGE_NUMBERS("page-numbers", EnumSet.of(FileKind.PDF), 1, 1),
    PROTECT_PDF("protect-pdf", EnumSet.of(FileKind.PDF), 1, 1),
    UNLOCK_PDF("unlock-pdf", EnumSet.of(FileKind.PDF), 1, 1),
    PDF_TO_EXCEL("pdf-to-excel", EnumSet.of(FileKind.PDF), 1, 1),
    /** One PDF plus the images placed in it; the changes come as the JSON "data" part. */
    EDIT_PDF("edit-pdf", EnumSet.of(FileKind.PDF, FileKind.PNG, FileKind.JPEG, FileKind.WEBP), 1, 21),
    PDF_TO_IMAGES("pdf-to-images", EnumSet.of(FileKind.PDF), 1, 1),
    IMAGES_TO_PDF("images-to-pdf", EnumSet.of(FileKind.PNG, FileKind.JPEG, FileKind.WEBP), 1, 20),
    COMPRESS_IMAGE("compress-image", EnumSet.of(FileKind.PNG, FileKind.JPEG, FileKind.WEBP), 1, 1),
    JPG_TO_PNG("jpg-to-png", EnumSet.of(FileKind.JPEG), 1, 1),
    PNG_TO_JPG("png-to-jpg", EnumSet.of(FileKind.PNG), 1, 1),
    WEBP_CONVERTER("webp-converter", EnumSet.of(FileKind.PNG, FileKind.JPEG, FileKind.WEBP), 1, 1),
    PDF_TO_WORD("pdf-to-word", EnumSet.of(FileKind.PDF), 1, 1),
    WORD_TO_PDF("word-to-pdf", EnumSet.of(FileKind.DOCX, FileKind.DOC), 1, 1),
    IMAGE_TO_TEXT("image-to-text", EnumSet.of(FileKind.PNG, FileKind.JPEG, FileKind.WEBP), 1, 1),
    SCANNED_PDF_OCR("scanned-pdf-ocr", EnumSet.of(FileKind.PDF), 1, 1),
    AI_SUMMARY("ai-summary", EnumSet.of(FileKind.PDF), 1, 1),
    DOCUMENT_INSIGHTS("document-insights", EnumSet.of(FileKind.PDF), 1, 1),
    /** Prepares a document for questions; answers come from POST /api/v1/jobs/{id}/chat. */
    CHAT_WITH_PDF("chat-with-pdf", EnumSet.of(FileKind.PDF), 1, 1);

    private final String slug;
    private final Set<FileKind> acceptedKinds;
    private final int minFiles;
    private final int maxFiles;

    ToolCode(String slug, Set<FileKind> acceptedKinds, int minFiles, int maxFiles) {
        this.slug = slug;
        this.acceptedKinds = acceptedKinds;
        this.minFiles = minFiles;
        this.maxFiles = maxFiles;
    }

    public String getSlug() {
        return slug;
    }

    public Set<FileKind> getAcceptedKinds() {
        return acceptedKinds;
    }

    public int getMinFiles() {
        return minFiles;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public static Optional<ToolCode> fromSlug(String slug) {
        return Arrays.stream(values()).filter(t -> t.slug.equals(slug)).findFirst();
    }
}
