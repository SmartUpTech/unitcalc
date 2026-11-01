package net.smartlogic.unitconverter.graphy.export

import android.content.Context
import android.net.Uri
import net.smartlogic.unitconverter.graphy.model.GraphyOutput
import net.smartlogic.unitconverter.graphy.renderer.GraphyDocumentRenderer
import net.smartlogic.unitconverter.helper.PreparedDocumentFiles
import java.io.File

/** Private frozen snapshots use the same save/cancel lifecycle as calculator reports. */
internal object GraphyExportFiles {
    fun create(context: Context, graph: GraphyOutput, width: Int, format: GraphyDocumentRenderer.Format): File =
        PreparedDocumentFiles.create(context, "graphy_exports", "graphy-", format.extension) {
            GraphyDocumentRenderer.prepare(context, graph, width).write(it, format)
        }
    fun discard(context: Context, path: String) =
        PreparedDocumentFiles.discard(context, "graphy_exports", "graphy-", path)
    fun save(context: Context, path: String, uri: Uri) =
        PreparedDocumentFiles.save(context, "graphy_exports", "graphy-", path, uri)
}
