package net.smartlogic.unitconverter.graphy.export

import android.app.Application
import android.net.Uri
import net.smartlogic.unitconverter.graphy.integration.CalculationSnapshot
import net.smartlogic.unitconverter.graphy.integration.GraphyBridge
import net.smartlogic.unitconverter.graphy.renderer.GraphyDocumentRenderer.Format
import net.smartlogic.unitconverter.utils.ExpressionEvaluator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GraphyExportFilesTest {
    private fun graph() = GraphyBridge().build(CalculationSnapshot.create("basic", "20-5-3", "20-5-3", "12", ExpressionEvaluator.evaluate("20-5-3")))
    @Test fun preparedSnapshotSavesExactlyAndCancelRemovesOnlyOwnedTemporaryFiles() {
        val context = RuntimeEnvironment.getApplication()
        val first = GraphyExportFiles.create(context, graph(), 360, Format.PNG)
        val bytes = first.readBytes()
        val target = File(context.filesDir, "saved-graph.png")
        GraphyExportFiles.save(context, first.absolutePath, Uri.fromFile(target))
        assertArrayEquals(bytes, target.readBytes())
        assertFalse(first.exists())
        val cancelled = GraphyExportFiles.create(context, graph(), 360, Format.PNG)
        GraphyExportFiles.discard(context, cancelled.absolutePath)
        assertFalse(cancelled.exists())
        try { GraphyExportFiles.discard(context, target.absolutePath); fail("Must reject a non-export file") }
        catch (_: IOException) { assertTrue(target.exists()) }
    }
    @Test fun failedSaveRemovesTheSnapshot() {
        val context = RuntimeEnvironment.getApplication()
        val prepared = GraphyExportFiles.create(context, graph(), 360, Format.PNG)
        try { GraphyExportFiles.save(context, prepared.absolutePath, Uri.parse("unsupported://missing/file")); fail("Missing provider must fail") }
        catch (_: Exception) { assertFalse(prepared.exists()) }
    }
}
