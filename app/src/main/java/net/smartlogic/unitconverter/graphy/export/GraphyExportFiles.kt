package net.smartlogic.unitconverter.graphy.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.model.GraphyOutput
import net.smartlogic.unitconverter.graphy.renderer.GraphyDocumentRenderer
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Private prepared snapshots survive the save picker; no shared-storage permission is needed. */
internal object GraphyExportFiles {
    fun create(context: Context, graph: GraphyOutput, width: Int, format: GraphyDocumentRenderer.Format): File {
        val directory = File(context.cacheDir, "graphy_exports")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create export cache")
        val oldest = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(context.resources.getInteger(R.integer.graphy_export_cache_hours).toLong())
        directory.listFiles()?.filter { it.name.startsWith("graphy-") && it.lastModified() < oldest }?.forEach { it.delete() }
        val file = File.createTempFile("graphy-", ".${format.extension}", directory)
        try {
            file.outputStream().use { GraphyDocumentRenderer.prepare(context, graph, width).write(it, format) }
            return file
        } catch (failure: Exception) { file.delete(); throw failure }
    }

    private fun ownedFile(context: Context, path: String): File {
        val file = File(path).canonicalFile
        val directory = File(context.cacheDir, "graphy_exports").canonicalFile
        if (file.parentFile != directory || !file.name.startsWith("graphy-")) throw IOException("Invalid export snapshot")
        return file
    }

    fun discard(context: Context, path: String) { ownedFile(context, path).delete() }

    fun save(context: Context, path: String, uri: Uri) {
        val file = ownedFile(context, path)
        try {
            val destination = context.contentResolver.openOutputStream(uri, "w") ?: throw IOException("Cannot open export destination")
            destination.use { output -> file.inputStream().use { it.copyTo(output) } }
        } catch (failure: Exception) {
            // ACTION_CREATE_DOCUMENT creates a new target. Remove a partial write, never an unrelated existing file.
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
            throw failure
        } finally { file.delete() }
    }
}
