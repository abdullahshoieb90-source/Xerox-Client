package com.origin.launcher.bedrock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Legacy visual modules ported to Bedrock's normal resource-pack format. */
public enum BedrockResourceModule {
    XELO_TITLE("xelo_title", "Xelo Title", "2b95d269-ffb8-4460-8c4d-74f95f72954e",
            "it replaces the classic title texture", "textures/ui/title.png", "xelo_title.png"),
    NO_PUMPKIN_OVERLAY("no_pumpkin_overlay", "No Pumpkin Overlay", "51c75d12-c1db-4ecd-8556-489bb8f60d0f",
            "it replaces the pumpkin screen overlay", "textures/misc/pumpkinblur.png", "transparent_overlay.png"),
    NO_SPYGLASS_OVERLAY("no_spyglass_overlay", "No Spyglass Overlay", "7b785c71-b939-4e2c-ae01-8ffa03dca1ed",
            "it replaces the spyglass overlay texture", "textures/ui/spyglass_scope.png", "transparent_overlay.png");

    private final String legacyKey;
    private final String displayName;
    private final String uuid;
    private final String description;
    private final String texturePath;
    private final String textureAsset;

    BedrockResourceModule(String legacyKey, String displayName, String uuid, String description,
                          String texturePath, String textureAsset) {
        this.legacyKey = legacyKey;
        this.displayName = displayName;
        this.uuid = uuid;
        this.description = description;
        this.texturePath = texturePath;
        this.textureAsset = textureAsset;
    }

    public static BedrockResourceModule forLegacyKey(String legacyKey) {
        for (BedrockResourceModule module : values()) {
            if (module.legacyKey.equals(legacyKey)) return module;
        }
        return null;
    }

    String getTextureAsset() {
        return "bedrock_modules/" + textureAsset;
    }

    void writePack(OutputStream output, InputStream texture) throws IOException {
        String moduleUuid = java.util.UUID.nameUUIDFromBytes(
                (uuid + "/resources").getBytes(StandardCharsets.UTF_8)).toString();
        // All substituted values are fixed enum constants, not supplied by users.
        String manifest = "{\"format_version\":2,\"header\":{\"name\":\"Xelo - " + displayName
                + "\",\"description\":\"Xelo resource pack: " + description + "\",\"uuid\":\"" + uuid
                + "\",\"version\":[1,0,0],\"min_engine_version\":[1,26,60]},\"modules\":[{"
                + "\"type\":\"resources\",\"uuid\":\"" + moduleUuid + "\",\"version\":[1,0,0]}]}";
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(manifest.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(texturePath));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = texture.read(buffer)) != -1) zip.write(buffer, 0, read);
            zip.closeEntry();
        }
    }
}
