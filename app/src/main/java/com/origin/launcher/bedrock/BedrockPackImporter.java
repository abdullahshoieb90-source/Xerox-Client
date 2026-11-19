package com.origin.launcher.bedrock;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.origin.launcher.Launcher.MinecraftLauncher;
import com.origin.launcher.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntConsumer;

/** Hands only explicitly selected documents and built-in packs to the installed game. */
public final class BedrockPackImporter implements AutoCloseable {
    private final Activity activity;
    private final Lifecycle viewLifecycle;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private boolean closed;
    private boolean busy;

    public BedrockPackImporter(Activity activity, LifecycleOwner viewOwner) {
        this.activity = activity;
        this.viewLifecycle = viewOwner.getLifecycle();
    }

    /** Result is zero when Minecraft's activity is opened; only the game can confirm import. */
    public void openDocument(Uri uri, IntConsumer finished) {
        if (!begin()) return;
        if (uri == null || !ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            finish(finished, R.string.bedrock_unsupported_file);
            return;
        }
        worker.execute(() -> {
            ContentResolver resolver = activity.getContentResolver();
            String name = displayName(uri);
            String extension = BedrockFiles.extensionOf(name == null ? uri.getLastPathSegment() : name);
            if (extension == null) {
                activity.runOnUiThread(() -> finish(finished, R.string.bedrock_unsupported_file));
                return;
            }
            String mimeType;
            try {
                mimeType = resolver.getType(uri);
            } catch (RuntimeException ignored) {
                mimeType = null;
            }
            String hints = mimeType;
            activity.runOnUiThread(() -> {
                if (!isOpen()) return;
                if (!canHandOff()) {
                    finish(finished, R.string.bedrock_import_canceled);
                    return;
                }
                if (BedrockFiles.canForwardOriginal(uri.getPath(), extension) && send(uri, hints)) {
                    finish(finished, 0);
                } else {
                    worker.execute(() -> stageAndSend(() -> resolver.openInputStream(uri), extension, hints, finished));
                }
            });
        });
    }

    public void openModule(BedrockResourceModule module, IntConsumer finished) {
        if (!begin()) return;
        worker.execute(() -> stageAndSend(() -> {
            ByteArrayOutputStream pack = new ByteArrayOutputStream();
            try (InputStream texture = activity.getAssets().open(module.getTextureAsset())) {
                module.writePack(pack, texture);
            }
            return new ByteArrayInputStream(pack.toByteArray());
        }, ".mcpack", null, finished));
    }

    private void stageAndSend(DocumentSource source, String extension, String type, IntConsumer finished) {
        File staged = null;
        int error = 0;
        try (InputStream in = source.open()) {
            if (in == null) throw new IOException("Selected file unavailable");
            staged = BedrockImportCache.stage(BedrockFileProvider.directory(activity), extension, in,
                    BedrockImportCache.MAX_CACHE_BYTES, System.currentTimeMillis());
        } catch (BedrockImportCache.CacheLimitException ignored) {
            error = R.string.bedrock_cache_limit;
        } catch (IOException | RuntimeException ignored) {
            error = R.string.bedrock_read_failed;
        }
        File pending = staged;
        int failure = error;
        activity.runOnUiThread(() -> {
            if (!isOpen() || failure != 0) {
                if (pending != null) pending.delete();
                finish(finished, failure);
                return;
            }
            if (!canHandOff()) {
                pending.delete();
                finish(finished, R.string.bedrock_import_canceled);
                return;
            }
            boolean launched = send(BedrockFileProvider.uriFor(activity, pending), type);
            if (!launched) pending.delete();
            finish(finished, launched ? 0 : R.string.bedrock_handoff_failed);
        });
    }

    private boolean send(Uri uri, String originalType) {
        for (String mimeType : BedrockFiles.handoffMimeTypes(originalType)) {
            Intent view = new Intent(Intent.ACTION_VIEW)
                    .setPackage(MinecraftLauncher.MC_PACKAGE_NAME)
                    .setDataAndType(uri, mimeType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            view.setClipData(ClipData.newRawUri("Bedrock import", uri));
            try {
                activity.startActivity(view);
                return true;
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // Try the next registered MIME type; never redirect the file to another app.
            }
        }
        return false;
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = activity.getContentResolver().query(uri,
                new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) return cursor.getString(column);
            }
        } catch (RuntimeException ignored) {
            // A picker provider can omit the display name or go away before the query.
        }
        return null;
    }

    private boolean isOpen() {
        return !closed && !activity.isFinishing() && !activity.isDestroyed();
    }

    private boolean canHandOff() {
        return isOpen() && viewLifecycle.getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
    }

    private boolean begin() {
        if (!isOpen() || busy) return false;
        busy = true;
        return true;
    }

    private void finish(IntConsumer callback, int error) {
        busy = false;
        if (isOpen()) callback.accept(error);
    }

    @Override
    public void close() {
        closed = true;
        worker.shutdownNow();
    }

    private interface DocumentSource {
        InputStream open() throws IOException;
    }
}
