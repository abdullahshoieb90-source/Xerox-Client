package com.origin.launcher.bedrock;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class BedrockImportCacheTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void createsShareableFileWithTheExtensionAndOriginalBytes() throws Exception {
        File cache = folder.newFolder("imports");
        byte[] bytes = {80, 75, 3, 4, 0, 1};
        File staged = BedrockImportCache.stage(cache, ".mcpack", new ByteArrayInputStream(bytes),
                6, System.currentTimeMillis());

        assertTrue(staged.getName().endsWith(".mcpack"));
        File resolved = BedrockImportCache.resolve(cache, staged.getName());
        assertEquals(staged.getCanonicalFile(), resolved);
        assertArrayEquals(bytes, Files.readAllBytes(resolved.toPath()));
    }

    @Test
    public void rejectsOversizeCopiesAndRemovesTheIncompleteFile() throws Exception {
        File cache = folder.newFolder("imports");
        assertThrows(BedrockImportCache.CacheLimitException.class, () ->
                BedrockImportCache.stage(cache, ".mcpack", new ByteArrayInputStream(new byte[4]),
                        3, System.currentTimeMillis()));
        assertEquals(0, cache.list().length);
    }

    @Test
    public void prunesOldStagedFilesBeforeCountingTheCacheBudget() throws Exception {
        File cache = folder.newFolder("imports");
        long now = System.currentTimeMillis();
        File expired = BedrockImportCache.stage(cache, ".mcaddon", new ByteArrayInputStream(new byte[4]),
                4, now);
        assertTrue(expired.setLastModified(now - BedrockImportCache.RETENTION_MILLIS - 1_000));
        File current = BedrockImportCache.stage(cache, ".mcworld", new ByteArrayInputStream(new byte[4]),
                4, now);
        assertFalse(expired.exists());
        assertTrue(current.exists());
        assertThrows(BedrockImportCache.CacheLimitException.class, () ->
                BedrockImportCache.stage(cache, ".mcworld", new ByteArrayInputStream(new byte[1]), 4, now));
    }

    @Test
    public void onlyResolvesStagedNamesInsideTheCache() throws Exception {
        File cache = folder.newFolder("imports");
        folder.newFile("world.mcworld");
        Files.write(new File(cache, "unrelated.mcpack").toPath(), new byte[] {1});
        assertThrows(FileNotFoundException.class, () -> BedrockImportCache.resolve(cache, "../world.mcworld"));
        assertThrows(FileNotFoundException.class, () -> BedrockImportCache.resolve(cache, "unrelated.mcpack"));
        assertThrows(FileNotFoundException.class, () ->
                BedrockImportCache.resolve(cache, "bedrock-00000000000000000000000000000000.mcpack"));
    }
}
