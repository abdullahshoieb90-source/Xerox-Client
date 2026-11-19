package com.origin.launcher.bedrock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Bounded, private staging area for files chosen in the Android document picker. */
final class BedrockImportCache {
    static final long MAX_CACHE_BYTES = 512L * 1024 * 1024;
    static final long RETENTION_MILLIS = TimeUnit.HOURS.toMillis(24);
    private static final Pattern STAGED_STEM = Pattern.compile("bedrock-[0-9a-f]{32}");

    private BedrockImportCache() {}

    static synchronized File stage(File directory, String extension, InputStream source,
                                   long budget, long now) throws IOException {
        if (extension == null || !extension.equals(BedrockFiles.extensionOf(extension))) {
            throw new IllegalArgumentException("Unsupported Bedrock extension");
        }
        Files.createDirectories(directory.toPath());
        long remaining = budget;
        File[] entries = directory.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                if (!isStagedName(entry.getName()) || !entry.isFile()) continue;
                if (now - entry.lastModified() >= RETENTION_MILLIS && entry.delete()) continue;
                remaining -= entry.length();
            }
        }
        if (remaining <= 0) throw new CacheLimitException();

        File staged = new File(directory, "bedrock-"
                + UUID.randomUUID().toString().replace("-", "") + extension);
        if (!staged.createNewFile()) throw new IOException("Could not create import cache file");
        boolean copied = false;
        try {
            try (FileOutputStream output = new FileOutputStream(staged)) {
                byte[] buffer = new byte[64 * 1024];
                long written = 0;
                int read;
                while ((read = source.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                    if (read > remaining - written) throw new CacheLimitException();
                    output.write(buffer, 0, read);
                    written += read;
                }
                if (written == 0) throw new IOException("Selected file is empty");
            }
            copied = true;
            return staged;
        } finally {
            if (!copied) staged.delete();
        }
    }

    static File resolve(File directory, String name) throws FileNotFoundException {
        if (!isStagedName(name)) throw new FileNotFoundException("Invalid import file name");
        try {
            File expected = new File(directory.getCanonicalFile(), name);
            File actual = expected.getCanonicalFile();
            if (actual.equals(expected) && actual.isFile()) return actual;
        } catch (IOException ignored) {
            // Do not expose the private cache path to callers.
        }
        throw new FileNotFoundException("Import file is no longer available");
    }

    private static boolean isStagedName(String name) {
        String extension = BedrockFiles.extensionOf(name);
        return extension != null && name.endsWith(extension)
                && STAGED_STEM.matcher(name.substring(0, name.length() - extension.length())).matches();
    }

    static final class CacheLimitException extends IOException {}
}
