package com.docsaas.api.processing;

import java.text.DecimalFormat;

public final class FileNames {

    private FileNames() {
    }

    /** Original filename without extension, reduced to safe characters for download names. */
    public static String baseName(String originalName) {
        if (originalName == null) {
            return "document";
        }
        String name = originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        name = name.replaceAll("[^\\p{L}\\p{N} ._()-]", "_").trim();
        if (name.length() > 80) {
            name = name.substring(0, 80);
        }
        return name.isEmpty() ? "document" : name;
    }

    public static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        DecimalFormat df = new DecimalFormat("0.#");
        if (bytes < 1024 * 1024) {
            return df.format(bytes / 1024.0) + " KB";
        }
        return df.format(bytes / (1024.0 * 1024.0)) + " MB";
    }

    public static String sizeChange(long before, long after) {
        if (after >= before) {
            return "File is already well optimized (" + humanSize(before) + "); no further reduction was possible.";
        }
        long pct = Math.round((before - after) * 100.0 / before);
        return "Reduced from " + humanSize(before) + " to " + humanSize(after) + " (" + pct + "% smaller).";
    }
}
