package com.origin.launcher.bedrock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class BedrockFiles {
    static final String BINARY_MIME_TYPE = "application/octet-stream";
    private static final String[] EXTENSIONS = {".mcpack", ".mcaddon", ".mcworld", ".mctemplate"};

    private BedrockFiles() {}

    static String extensionOf(String displayName) {
        if (displayName == null) return null;
        String name = displayName.toLowerCase(Locale.ROOT);
        for (String extension : EXTENSIONS) {
            if (name.endsWith(extension)) return extension;
        }
        return null;
    }

    static boolean canForwardOriginal(String decodedPath, String extension) {
        // Android intent filters can match the URI path, not the provider's display name.
        return decodedPath != null && extension != null && decodedPath.endsWith(extension);
    }

    static List<String> handoffMimeTypes(String providerType) {
        Set<String> types = new LinkedHashSet<>();
        types.add(BINARY_MIME_TYPE);
        if (providerType != null && providerType.matches("[a-z0-9.+-]+/[a-z0-9.+_-]+")) {
            types.add(providerType);
        }
        types.add("application/zip");
        return new ArrayList<>(types);
    }
}
