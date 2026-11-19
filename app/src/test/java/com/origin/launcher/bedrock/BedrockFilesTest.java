package com.origin.launcher.bedrock;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BedrockFilesTest {
    @Test
    public void acceptsBedrockDocumentExtensions() {
        assertEquals(".mcpack", BedrockFiles.extensionOf("my textures.mcpack"));
        assertEquals(".mcaddon", BedrockFiles.extensionOf("bundle.mcaddon"));
        assertEquals(".mcworld", BedrockFiles.extensionOf("world.mcworld"));
        assertEquals(".mctemplate", BedrockFiles.extensionOf("template.mctemplate"));
    }

    @Test
    public void normalizesUppercaseExtensionsInLocalizedNames() {
        assertEquals(".mcworld", BedrockFiles.extensionOf("عالمي.MCWORLD"));
    }

    @Test
    public void rejectsOtherFormatsAndDoubleExtensions() {
        assertNull(BedrockFiles.extensionOf("download.apk"));
        assertNull(BedrockFiles.extensionOf("archive.zip"));
        assertNull(BedrockFiles.extensionOf("world.mcworld.zip"));
        assertNull(BedrockFiles.extensionOf("mcworld"));
    }

    @Test
    public void rejectsMissingNames() {
        assertNull(BedrockFiles.extensionOf(null));
        assertNull(BedrockFiles.extensionOf(""));
    }

    @Test
    public void stagesOpaqueDownloadUrisAndNormalizesExtensionsInUris() {
        assertFalse(BedrockFiles.canForwardOriginal("/document/msf:1361", ".mcpack"));
        assertFalse(BedrockFiles.canForwardOriginal("/document/Textures.MCPACK", ".mcpack"));
        assertFalse(BedrockFiles.canForwardOriginal("/document/world.mcworld", ".mcpack"));
        assertTrue(BedrockFiles.canForwardOriginal("/document/raw:/Download/Textures.mcpack", ".mcpack"));
    }

    @Test
    public void triesKnownAndProviderMimeTypesWithoutDuplicatesOrWildcards() {
        assertEquals(Arrays.asList("application/octet-stream", "application/zip"),
                BedrockFiles.handoffMimeTypes("application/zip"));
        assertEquals(Arrays.asList("application/octet-stream", "application/x-zip-compressed", "application/zip"),
                BedrockFiles.handoffMimeTypes("application/x-zip-compressed"));
        assertEquals(Arrays.asList("application/octet-stream", "application/zip"),
                BedrockFiles.handoffMimeTypes("*/*"));
    }
}
