package com.origin.launcher.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

public class BedrockHotbarPackTest {
    private static final String[] HOTBAR_IDS = {
            ModIds.HOTBAR_ONE, ModIds.HOTBAR_TWO, ModIds.HOTBAR_THREE, ModIds.HOTBAR_FOUR,
            ModIds.HOTBAR_FIVE, ModIds.HOTBAR_SIX, ModIds.HOTBAR_SEVEN, ModIds.HOTBAR_EIGHT,
            ModIds.HOTBAR_NINE
    };

    @Test
    public void everySupportedLegacyIdExportsItsOwnNumberAndVanillaAction() throws IOException {
        for (int index = 0; index < HOTBAR_IDS.length; index++) {
            int slot = index + 1;
            String id = HOTBAR_IDS[index];
            assertTrue(BedrockHotbarPack.supports(id));
            BedrockHotbarPack pack = BedrockHotbarPack.fromSelectedMods(Arrays.asList(id, ModIds.ZOOM));
            assertEquals(1 << index, pack.getSelectionMask());
            JsonArray controls = rowControls(readPack(pack).get("ui/hud_screen.json"));
            assertEquals(1, controls.size());
            assertShortcut(controls.get(0).getAsJsonObject(), slot);
        }
    }

    @Test
    public void snapshotKeepsOnlyChosenHotbarSlotsInNumericOrder() throws IOException {
        ArrayList<String> selected = new ArrayList<>(Arrays.asList(ModIds.HOTBAR_NINE,
                ModIds.QUICK_DROP, ModIds.HOTBAR_ONE, ModIds.HOTBAR_FIVE, ModIds.HOTBAR_ONE));
        BedrockHotbarPack pack = BedrockHotbarPack.fromSelectedMods(selected);
        selected.clear();
        assertEquals("1, 5, 9", pack.getSlotsLabel());
        assertEquals("xelo-hotbar-1-5-9.mcpack", pack.getFileName());
        JsonArray controls = rowControls(readPack(pack).get("ui/hud_screen.json"));
        assertEquals(3, controls.size());
        assertShortcut(controls.get(0).getAsJsonObject(), 1);
        assertShortcut(controls.get(1).getAsJsonObject(), 5);
        assertShortcut(controls.get(2).getAsJsonObject(), 9);
    }

    @Test
    public void emptyOrPreviewOnlySelectionsCannotWriteAMcpack() throws IOException {
        assertFalse(BedrockHotbarPack.supports(ModIds.QUICK_DROP));
        assertFalse(BedrockHotbarPack.supports(ModIds.AUTO_SPRINT));
        assertFalse(BedrockHotbarPack.supports(ModIds.ZOOM));
        assertFalse(BedrockHotbarPack.supports(null));
        BedrockHotbarPack pack = BedrockHotbarPack.fromSelectedMods(
                Arrays.asList(ModIds.QUICK_DROP, ModIds.AUTO_SPRINT, ModIds.ZOOM, "old_mod"));
        assertFalse(pack.hasSlots());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream template = openTemplate()) {
            assertThrows(IllegalArgumentException.class, () -> pack.writePack(output, template));
        }
        assertEquals(0, output.size());
        assertThrows(IllegalArgumentException.class, () -> new BedrockHotbarPack(1 << 9));
    }

    @Test
    public void packAddsOnlyItsOwnTouchControlsAndUsesPressedMapping() throws IOException {
        Map<String, JsonObject> entries = readPack(new BedrockHotbarPack(0b111111111));
        assertEquals(2, entries.size());
        assertNotNull(entries.get("manifest.json"));
        JsonObject hud = entries.get("ui/hud_screen.json");
        assertNotNull(hud);
        assertEquals("hud", hud.get("namespace").getAsString());
        JsonObject root = hud.getAsJsonObject("root_panel");
        assertEquals(1, root.size()); // Never replace stock controls or bindings.
        JsonObject mod = root.getAsJsonArray("modifications").get(0).getAsJsonObject();
        assertEquals("controls", mod.get("array_name").getAsString());
        assertEquals("insert_back", mod.get("operation").getAsString());
        JsonObject inserted = mod.getAsJsonArray("value").get(0).getAsJsonObject()
                .getAsJsonObject("xelo_hotbar_shortcuts@hud.xelo_hotbar_shortcuts");
        assertEquals("(not $touch)", inserted.get("ignored").getAsString());
        JsonObject binding = hud.getAsJsonObject("xelo_hotbar_shortcuts").getAsJsonArray("bindings")
                .get(0).getAsJsonObject();
        assertEquals("#hotbar_visible", binding.get("binding_name").getAsString());
        assertEquals("#visible", binding.get("binding_name_override").getAsString());
        assertEquals(9, rowControls(hud).size());
        JsonArray mappings = hud.getAsJsonObject("xelo_hotbar_button@common.button")
                .getAsJsonArray("button_mappings");
        assertEquals(1, mappings.size());
        JsonObject mapping = mappings.get(0).getAsJsonObject();
        assertEquals("button.menu_select", mapping.get("from_button_id").getAsString());
        assertEquals("pressed", mapping.get("mapping_type").getAsString());
        assertEquals("$pressed_button_name", mapping.get("to_button_id").getAsString());
        assertTrue(mapping.get("consume_event").getAsBoolean());
    }

    @Test
    public void identitiesAreStableForSameSelectionAndDistinctForDifferentSelections() throws IOException {
        JsonObject manifest = readPack(new BedrockHotbarPack(5)).get("manifest.json");
        JsonObject header = manifest.getAsJsonObject("header");
        JsonObject module = manifest.getAsJsonArray("modules").get(0).getAsJsonObject();
        assertEquals(2, manifest.get("format_version").getAsInt());
        assertEquals("[1,26,60]", header.getAsJsonArray("min_engine_version").toString());
        assertEquals("Xelo - Hotbar (1, 3)", header.get("name").getAsString());
        assertEquals("resources", module.get("type").getAsString());
        assertNotEquals(UUID.fromString(header.get("uuid").getAsString()),
                UUID.fromString(module.get("uuid").getAsString()));
        assertEquals(manifest, readPack(new BedrockHotbarPack(5)).get("manifest.json"));
        JsonObject otherHeader = readPack(new BedrockHotbarPack(2)).get("manifest.json")
                .getAsJsonObject("header");
        assertNotEquals(header.get("uuid"), otherHeader.get("uuid"));
    }

    private static JsonArray rowControls(JsonObject hud) {
        return hud.getAsJsonObject("xelo_hotbar_shortcuts").getAsJsonArray("controls");
    }

    private static void assertShortcut(JsonObject control, int slot) {
        JsonObject button = control.getAsJsonObject("xelo_slot_" + slot + "@hud.xelo_hotbar_button");
        assertNotNull(button);
        assertEquals("button.slot_" + slot, button.get("$pressed_button_name").getAsString());
        assertEquals(Integer.toString(slot), button.get("$xelo_slot_number").getAsString());
    }

    private static InputStream openTemplate() throws IOException {
        return Files.newInputStream(Paths.get("src/main/assets", BedrockHotbarPack.UI_TEMPLATE_ASSET));
    }

    private static Map<String, JsonObject> readPack(BedrockHotbarPack pack) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream template = openTemplate()) {
            pack.writePack(output, template);
        }
        Map<String, JsonObject> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(zip, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                assertNull(entries.put(entry.getName(), json));
            }
        }
        return entries;
    }
}
