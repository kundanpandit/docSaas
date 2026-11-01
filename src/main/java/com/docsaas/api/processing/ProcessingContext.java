package com.docsaas.api.processing;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.docsaas.api.enums.FileKind;

/**
 * @param data JSON sent with the job for tools that take structured input (e.g. the PDF editor's
 *             list of changes); null for other tools
 */
public record ProcessingContext(List<InputFile> inputs, Map<String, String> options, Path workDir, Path data) {

    public ProcessingContext(List<InputFile> inputs, Map<String, String> options, Path workDir) {
        this(inputs, options, workDir, null);
    }

    public record InputFile(Path path, String originalName, FileKind kind) {
    }

    public String option(String key, String defaultValue) {
        return option(options, key, defaultValue);
    }

    public static String option(Map<String, String> options, String key, String defaultValue) {
        String value = options.get(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    /** Reads an option that must be one of the allowed values. */
    public static String choice(Map<String, String> options, String key, String defaultValue, String... allowed) {
        String value = option(options, key, defaultValue).toLowerCase();
        for (String a : allowed) {
            if (a.equals(value)) {
                return value;
            }
        }
        throw new ProcessingException("INVALID_OPTION",
                "Invalid value for \"" + key + "\". Allowed: " + String.join(", ", allowed) + ".");
    }

    public String choice(String key, String defaultValue, String... allowed) {
        return choice(options, key, defaultValue, allowed);
    }
}
