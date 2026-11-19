package com.origin.launcher.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Immutable snapshot of the legacy hotbar selections to export for Bedrock's JSON UI. */
public final class BedrockHotbarPack {
    static final String UI_TEMPLATE_ASSET = "bedrock_modules/xelo_hotbar_hud.json";
    private static final String[] HOTBAR_IDS = {
            ModIds.HOTBAR_ONE, ModIds.HOTBAR_TWO, ModIds.HOTBAR_THREE,
            ModIds.HOTBAR_FOUR, ModIds.HOTBAR_FIVE, ModIds.HOTBAR_SIX,
            ModIds.HOTBAR_SEVEN, ModIds.HOTBAR_EIGHT, ModIds.HOTBAR_NINE
    };
    private static final int ALL_SLOTS = (1 << HOTBAR_IDS.length) - 1;
    private static final int TILE_WIDTH = 28;
    private static final int ROW_HEIGHT = 26;
    private final int selectionMask;

    public BedrockHotbarPack(int selectionMask) {
        if ((selectionMask & ~ALL_SLOTS) != 0) throw new IllegalArgumentException("Invalid hotbar selection");
        this.selectionMask = selectionMask;
    }

    public static boolean supports(String modId) {
        for (String id : HOTBAR_IDS) {
            if (id.equals(modId)) return true;
        }
        return false;
    }

    public static BedrockHotbarPack fromSelectedMods(Collection<String> modIds) {
        int mask = 0;
        for (int index = 0; index < HOTBAR_IDS.length; index++) {
            if (modIds.contains(HOTBAR_IDS[index])) mask |= 1 << index;
        }
        return new BedrockHotbarPack(mask);
    }

    public boolean hasSlots() {
        return selectionMask != 0;
    }

    public int getSelectionMask() {
        return selectionMask;
    }

    public String getSlotsLabel() {
        return slotNumbers(", ");
    }

    public String getFileName() {
        return "xelo-hotbar-" + slotNumbers("-") + ".mcpack";
    }

    private String slotNumbers(String separator) {
        StringJoiner numbers = new StringJoiner(separator);
        for (int slot = 1; slot <= HOTBAR_IDS.length; slot++) {
            if (includes(slot)) numbers.add(Integer.toString(slot));
        }
        return numbers.toString();
    }

    private boolean includes(int slot) {
        return (selectionMask & (1 << (slot - 1))) != 0;
    }

    void writePack(OutputStream output, InputStream hudTemplate) throws IOException {
        if (!hasSlots()) throw new IllegalArgumentException("Select at least one hotbar slot");
        JsonObject hud = JsonParser.parseReader(new InputStreamReader(hudTemplate, StandardCharsets.UTF_8))
                .getAsJsonObject();
        JsonObject row = hud.getAsJsonObject("xelo_hotbar_shortcuts");
        int slots = Integer.bitCount(selectionMask);
        // Cap the row at 28 UI units per slot but let it shrink on narrow screens.
        row.add("max_size", numbers(slots * TILE_WIDTH, ROW_HEIGHT));
        String cellWidth = Double.toString(Math.floor(10000.0 / slots) / 100.0) + "%";
        JsonArray controls = row.getAsJsonArray("controls");
        for (int slot = 1; slot <= HOTBAR_IDS.length; slot++) {
            if (!includes(slot)) continue;
            JsonObject button = new JsonObject();
            JsonArray size = new JsonArray();
            size.add(cellWidth);
            size.add(ROW_HEIGHT);
            button.add("size", size);
            // Vanilla hud_screen routes the keyboard hotbar actions to button.slot_1 … slot_9.
            // Route a press on this HUD button to the same Minecraft action, not an Android key.
            button.addProperty("$pressed_button_name", "button.slot_" + slot);
            button.addProperty("$xelo_slot_number", Integer.toString(slot));
            JsonObject control = new JsonObject();
            control.add("xelo_slot_" + slot + "@hud.xelo_hotbar_button", button);
            controls.add(control);
        }
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            writeJson(zip, "manifest.json", manifest());
            writeJson(zip, "ui/hud_screen.json", hud);
        }
    }

    private JsonObject manifest() {
        // Different selections can coexist in My Packs; only one should be activated.
        // Re-exporting the same selection retains its identity. Bump the version on changes.
        String identity = "com.origin.launcher/bedrock/hotbar/" + selectionMask;
        JsonObject header = new JsonObject();
        header.addProperty("name", "Xelo - Hotbar (" + getSlotsLabel() + ")");
        header.addProperty("description", "Experimental touch shortcuts for the chosen hotbar slots. Activate only one Xelo Hotbar pack.");
        header.addProperty("uuid", stableId(identity));
        header.add("version", numbers(1, 0, 0));
        header.add("min_engine_version", numbers(1, 26, 60));
        JsonObject module = new JsonObject();
        module.addProperty("type", "resources");
        module.addProperty("uuid", stableId(identity + "/resources"));
        module.add("version", numbers(1, 0, 0));
        JsonArray modules = new JsonArray();
        modules.add(module);
        JsonObject manifest = new JsonObject();
        manifest.addProperty("format_version", 2);
        manifest.add("header", header);
        manifest.add("modules", modules);
        return manifest;
    }

    private static String stableId(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static JsonArray numbers(int... values) {
        JsonArray array = new JsonArray();
        for (int value : values) array.add(value);
        return array;
    }

    private static void writeJson(ZipOutputStream zip, String name, JsonObject json) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(json.toString().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
