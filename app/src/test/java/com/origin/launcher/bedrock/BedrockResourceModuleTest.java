package com.origin.launcher.bedrock;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
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
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
            // The bundled blank texture is an 8-bit RGBA, non-interlaced PNG with
            // zero-valued image data. Zero deltas reconstruct zero-alpha pixels
            // under each of the five PNG row filters.
            try (DataInputStream png = new DataInputStream(Files.newInputStream(
                    ASSETS.resolve(module.getTextureAsset())))) {
                assertEquals(0x89504E470D0A1A0AL, png.readLong());
                assertEquals(13, png.readInt());
                assertEquals(0x49484452, png.readInt()); // IHDR
                assertEquals(256, png.readInt());
                assertEquals(256, png.readInt());
                assertEquals(8, png.readUnsignedByte());
                assertEquals(6, png.readUnsignedByte()); // RGBA color type
                assertEquals(0, png.readUnsignedByte());
                assertEquals(0, png.readUnsignedByte());
                assertEquals(0, png.readUnsignedByte()); // No interlacing
                png.readInt(); // IHDR CRC

                ByteArrayOutputStream compressed = new ByteArrayOutputStream();
                while (true) {
                    int length = png.readInt();
                    int kind = png.readInt();
                    assertTrue(length >= 0 && length <= 4096);
                    byte[] data = new byte[length];
                    png.readFully(data);
                    png.readInt(); // Chunk CRC (ZIP test checks the bytes too).
                    if (kind == 0x49454E44) break; // IEND
                    if (kind == 0x49444154) compressed.write(data); // IDAT
                }
                try (DataInputStream rows = new DataInputStream(new InflaterInputStream(
                        new ByteArrayInputStream(compressed.toByteArray())))) {
                    byte[] row = new byte[256 * 4];
                    byte[] zero = new byte[row.length];
                    for (int y = 0; y < 256; y++) {
                        assertTrue(rows.readUnsignedByte() <= 4);
                        rows.readFully(row);
                        assertArrayEquals(zero, row);
                    }
                    assertEquals(-1, rows.read());
                }
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
