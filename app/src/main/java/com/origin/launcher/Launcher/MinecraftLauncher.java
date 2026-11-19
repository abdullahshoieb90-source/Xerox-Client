package com.origin.launcher.Launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;

/** Launches the user-installed Bedrock application using Android's package manager. */
public final class MinecraftLauncher {
    public static final String MC_PACKAGE_NAME = "com.mojang.minecraftpe";

    private MinecraftLauncher() {}

    public static boolean launch(Activity activity) {
        Intent intent = activity.getPackageManager().getLaunchIntentForPackage(MC_PACKAGE_NAME);
        if (intent == null) return false;
        try {
            activity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException ignored) {
            return false;
        }
    }
}
