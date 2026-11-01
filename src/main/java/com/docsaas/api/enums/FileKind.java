package com.docsaas.api.enums;

/** File types accepted by tools, detected from file content (magic bytes), not the extension. */
public enum FileKind {
    PDF("application/pdf", "pdf"),
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    WEBP("image/webp", "webp"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    DOC("application/msword", "doc");

    private final String mimeType;
    private final String extension;

    FileKind(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public boolean isImage() {
        return this == PNG || this == JPEG || this == WEBP;
    }
}
