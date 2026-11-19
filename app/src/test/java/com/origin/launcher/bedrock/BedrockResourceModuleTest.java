package com.origin.launcher.bedrock;

import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import static org.junit.Assert.*;

public class BedrockResourceModuleTest {
    private static final Path ASSETS = Paths.get("src/main/assets");
    private static final Pattern UUID_PROPERTY = Pattern.compile("\"uuid\":\"([^\"]+)\"");

    @Test
    public void mapsOnlyPortedLegacyCards() {
        assertSame(BedrockResourceModule.XELO_TITLE, BedrockResourceModule.forLegacyKey("xelo_title"));
        assertSame(BedrockResourceModule.NO_PUMPKIN_OVERLAY,
                BedrockResourceModule.forLegacyKey("no_pumpkin_overlay"));
        assertSame(BedrockResourceModule.NO_SPYGLASS_OVERLAY,
                BedrockResourceModule.forLegacyKey("no_spyglass_overlay"));
        assertNull(BedrockResourceModule.forLegacyKey("Nofog"));
        assertNull(BedrockResourceModule.forLegacyKey("inbuilt_mods_entry"));
        assertNull(BedrockResourceModule.forLegacyKey(null));
    }

    @Test
    public void packsHaveDistinctStableIdentitiesAndOnlyTheIntendedOverrides() throws IOException {
        Map<BedrockResourceModule, String> texturePaths = new HashMap<>();
        texturePaths.put(BedrockResourceModule.XELO_TITLE, "textures/ui/title.png");
        texturePaths.put(BedrockResourceModule.NO_PUMPKIN_OVERLAY, "textures/misc/pumpkinblur.png");
        texturePaths.put(BedrockResourceModule.NO_SPYGLASS_OVERLAY, "textures/ui/spyglass_scope.png");
        Set<UUID> ids = new HashSet<>();
        for (BedrockResourceModule module : BedrockResourceModule.values()) {
            Map<String, byte[]> entries = makePack(module);
            assertEquals(2, entries.size());
            byte[] manifest = entries.get("manifest.json");
            assertNotNull(manifest);
            String json = new String(manifest, StandardCharsets.UTF_8);
            assertTrue(json.contains("\"format_version\":2"));
            assertTrue(json.contains("\"type\":\"resources\""));
            assertTrue(json.contains("\"min_engine_version\":[1,26,60]"));
            assertArrayEquals(manifest, makePack(module).get("manifest.json"));
            Matcher matcher = UUID_PROPERTY.matcher(json);
            assertTrue(matcher.find());
            assertTrue(ids.add(UUID.fromString(matcher.group(1))));
            assertTrue(matcher.find());
            assertTrue(ids.add(UUID.fromString(matcher.group(1))));
            assertFalse(matcher.find());
            assertArrayEquals(Files.readAllBytes(ASSETS.resolve(module.getTextureAsset())),
                    entries.get(texturePaths.get(module)));
        }
    }

    @Test
    public void overlayPacksHaveTransparentTexturesWithVanillaDimensions() throws IOException {
        for (BedrockResourceModule module : new BedrockResourceModule[] {
                BedrockResourceModule.NO_PUMPKIN_OVERLAY, BedrockResourceModule.NO_SPYGLASS_OVERLAY}) {
            BufferedImage png = ImageIO.read(ASSETS.resolve(module.getTextureAsset()).toFile());
            assertNotNull(png);
            assertEquals(256, png.getWidth());
            assertEquals(256, png.getHeight());
            for (int argb : png.getRGB(0, 0, 256, 256, null, 0, 256)) {
                assertEquals(0, argb >>> 24);
            }
        }
    }

    private Map<String, byte[]> makePack(BedrockResourceModule module) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream texture = Files.newInputStream(ASSETS.resolve(module.getTextureAsset()))) {
            module.writePack(output, texture);
        }
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zip.read(buffer)) != -1) data.write(buffer, 0, read);
                assertNull(entries.put(entry.getName(), data.toByteArray()));
            }
        }
        return entries;
    }
}
