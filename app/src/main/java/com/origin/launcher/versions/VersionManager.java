package com.origin.launcher.versions;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.origin.launcher.Launcher.MinecraftLauncher;

import java.util.Collections;
import java.util.List;

/** Represents the single installed Bedrock package without scanning unrelated applications. */
public class VersionManager {
    private static VersionManager instance;
    private final Context context;
    private GameVersion installed;

    public static VersionManager get(Context context) {
        if (instance == null) instance = new VersionManager(context.getApplicationContext());
        return instance;
    }

    private VersionManager(Context context) {
        this.context = context;
        loadAllVersions();
    }

    public void loadAllVersions() {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(MinecraftLauncher.MC_PACKAGE_NAME, 0);
            String version = info.versionName == null ? Long.toString(
                    android.os.Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode)
                    : info.versionName;
            String label = info.applicationInfo == null ? "Minecraft"
                    : info.applicationInfo.loadLabel(pm).toString();
            installed = new GameVersion(info.packageName, label + " (" + version + ")", version,
                    null, true, info.packageName, null);
        } catch (PackageManager.NameNotFoundException | SecurityException ignored) {
            installed = null;
        }
    }

    public List<GameVersion> getInstalledVersions() {
        return installed == null ? Collections.emptyList() : Collections.singletonList(installed);
    }

    public GameVersion getSelectedVersion() {
        return installed;
    }
}
