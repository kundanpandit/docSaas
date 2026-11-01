package com.docsaas.api.processing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.docsaas.api.enums.FileKind;

/** Detects file type from leading bytes, so a renamed .exe is never accepted as a .pdf. */
public final class FileTypeDetector {

    /** Stop scanning a ZIP after this many entries (guards against crafted archives). */
    private static final int MAX_ZIP_ENTRIES = 2000;

    private FileTypeDetector() {
    }

    public static Optional<FileKind> detect(InputStream in) throws IOException {
        byte[] head = in.readNBytes(1024);
        return detect(head);
    }

    static Optional<FileKind> detect(byte[] head) {
        if (startsWith(head, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(FileKind.PNG);
        }
        if (startsWith(head, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(FileKind.JPEG);
        }
        if (startsWith(head, 'R', 'I', 'F', 'F') && head.length >= 12
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return Optional.of(FileKind.WEBP);
        }
        // ZIP container: only a candidate - confirm with isWordDocument() before trusting it
        if (startsWith(head, 'P', 'K', 0x03, 0x04)) {
            return Optional.of(FileKind.DOCX);
        }
        // OLE2 compound file (legacy .doc)
        if (startsWith(head, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1)) {
            return Optional.of(FileKind.DOC);
        }
        // PDF spec allows the header anywhere in the first 1024 bytes
        if (indexOf(head, "%PDF-".getBytes()) >= 0) {
            return Optional.of(FileKind.PDF);
        }
        return Optional.empty();
    }

    /** True if the ZIP stream is a Word document (contains word/document.xml). Reads entry names only. */
    public static boolean isWordDocument(InputStream in) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            int count = 0;
            while ((entry = zip.getNextEntry()) != null && count++ < MAX_ZIP_ENTRIES) {
                if ("word/document.xml".equals(entry.getName())) {
                    return true;
                }
            }
            return false;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(byte[] data, byte[] needle) {
        outer:
        for (int i = 0; i <= data.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
