package com.origin.launcher.bedrock;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/** Read-only provider for one staged document per granted URI. Not exported. */
public final class BedrockFileProvider extends ContentProvider {
    private static final String AUTHORITY_SUFFIX = ".bedrock";
    private static final String IMPORT_PATH = "import";

    static File directory(Context context) {
        return new File(context.getCacheDir(), "bedrock-imports");
    }

    static Uri uriFor(Context context, File staged) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(context.getPackageName() + AUTHORITY_SUFFIX)
                .appendPath(IMPORT_PATH)
                .appendPath(staged.getName())
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        // The MIME type is public metadata; do not disclose whether a file exists here.
        return BedrockFiles.BINARY_MIME_TYPE;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String[] columns = projection != null ? projection : new String[] {
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
        };
        MatrixCursor cursor = new MatrixCursor(columns, 1);
        try {
            File file = resolve(uri);
            Object[] values = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) values[i] = file.getName();
                if (OpenableColumns.SIZE.equals(columns[i])) values[i] = file.length();
            }
            cursor.addRow(values);
        } catch (FileNotFoundException ignored) {
            // Return an empty cursor for an expired file.
        }
        return cursor;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Import files are read-only");
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private File resolve(Uri uri) throws FileNotFoundException {
        Context context = getContext();
        List<String> segments = uri.getPathSegments();
        if (context == null || !ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())
                || !(context.getPackageName() + AUTHORITY_SUFFIX).equals(uri.getAuthority())
                || segments.size() != 2 || !IMPORT_PATH.equals(segments.get(0))) {
            throw new FileNotFoundException("Unknown import URI");
        }
        return BedrockImportCache.resolve(directory(context), segments.get(1));
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only provider");
    }
}
