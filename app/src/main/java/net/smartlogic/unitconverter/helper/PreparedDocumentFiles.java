package net.smartlogic.unitconverter.helper;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import net.smartlogic.unitconverter.R;

/** Frozen private documents shared by Graphy and reports. The caller owns a newly created URI. */
public final class PreparedDocumentFiles {
    @FunctionalInterface public interface Writer { void write(OutputStream output) throws IOException; }
    private PreparedDocumentFiles() { }
    public static File create(Context c, String directoryName, String prefix, String extension, Writer writer) throws IOException {
        File directory = new File(c.getCacheDir(), directoryName);
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Export cache unavailable");
        long expiry = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(c.getResources().getInteger(R.integer.graphy_export_cache_hours));
        File[] old = directory.listFiles();
        if (old != null) for (File file : old) if (file.getName().startsWith(prefix) && file.lastModified() < expiry) file.delete();
        File file = File.createTempFile(prefix, "." + extension, directory);
        try (OutputStream out = new FileOutputStream(file)) { writer.write(out); return file; }
        catch (IOException | RuntimeException failure) { file.delete(); throw failure; }
    }
    public static File owned(Context c, String directoryName, String prefix, String path) throws IOException {
        File file = new File(path).getCanonicalFile(), directory = new File(c.getCacheDir(), directoryName).getCanonicalFile();
        if (!directory.equals(file.getParentFile()) || !file.getName().startsWith(prefix)) throw new IOException("Invalid prepared document");
        return file;
    }
    public static void discard(Context c, String directory, String prefix, String path) throws IOException { owned(c, directory, prefix, path).delete(); }
    public static void save(Context c, String directory, String prefix, String path, Uri uri) throws IOException {
        File file = owned(c, directory, prefix, path);
        try (OutputStream destination = c.getContentResolver().openOutputStream(uri, "w")) {
            if (destination == null) throw new IOException("Destination unavailable");
            try (var input = new java.io.FileInputStream(file)) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = input.read(buffer)) != -1) destination.write(buffer, 0, count);
            }
        } catch (IOException | RuntimeException failure) {
            try { DocumentsContract.deleteDocument(c.getContentResolver(), uri); } catch (Exception ignored) { }
            throw failure;
        } finally { file.delete(); }
    }
}
