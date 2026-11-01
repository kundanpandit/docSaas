package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipSupport {

    private ZipSupport() {
    }

    public record Entry(Path file, String name) {
    }

    public static void zip(List<Entry> entries, Path target) throws IOException {
        Set<String> used = new HashSet<>();
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            for (Entry entry : entries) {
                String name = entry.name();
                for (int n = 2; !used.add(name); n++) {
                    name = n + "_" + entry.name();
                }
                zip.putNextEntry(new ZipEntry(name));
                Files.copy(entry.file(), zip);
                zip.closeEntry();
            }
        }
    }
}
