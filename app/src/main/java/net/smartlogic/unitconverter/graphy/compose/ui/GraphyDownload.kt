package net.smartlogic.unitconverter.graphy.compose.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.dimensionResource
import kotlinx.coroutines.*
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.export.GraphyExportFiles
import net.smartlogic.unitconverter.graphy.model.GraphyOutput
import net.smartlogic.unitconverter.graphy.renderer.GraphyDocumentRenderer.Format
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal data class GraphyDownloadState(val enabled: Boolean, val busy: Boolean, val prepare: (Format) -> Unit)

/** Keep picker registration and the frozen file above the graph-output key, including during rate updates. */
@Composable
internal fun rememberGraphyDownload(output: GraphyOutput?, viewportWidth: Int): GraphyDownloadState {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var pendingFile by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun failed(error: Exception) {
        val tooLarge = context.getString(R.string.graphy_export_png_too_large)
        Toast.makeText(context, if (error.message == tooLarge) tooLarge else context.getString(R.string.graphy_export_failed), Toast.LENGTH_LONG).show()
    }
    fun save(uri: Uri?) {
        val path = pendingFile ?: return
        scope.launch {
            busy = true
            try {
                // Finish the user-authorized write if the Graphy composition is removed during copying.
                withContext(NonCancellable + Dispatchers.IO) {
                    if (uri == null) GraphyExportFiles.discard(context, path) else GraphyExportFiles.save(context, path, uri)
                }
                if (uri != null) Toast.makeText(context, R.string.graphy_export_saved, Toast.LENGTH_SHORT).show()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { failed(error) }
            finally { pendingFile = null; busy = false }
        }
    }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(Format.PDF.mimeType), ::save)
    val png = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(Format.PNG.mimeType), ::save)
    return GraphyDownloadState(output != null && !output.isEmpty && !busy && pendingFile == null, busy) { format ->
        val snapshot = output
        if (snapshot != null && !snapshot.isEmpty && !busy && pendingFile == null) scope.launch {
            busy = true
            var prepared: File? = null
            try {
                withContext(Dispatchers.IO) { prepared = GraphyExportFiles.create(context, snapshot, viewportWidth, format) }
                pendingFile = prepared!!.absolutePath
                val name = "Graphy-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.${format.extension}"
                if (format == Format.PDF) pdf.launch(name) else png.launch(name)
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable + Dispatchers.IO) { prepared?.let { GraphyExportFiles.discard(context, it.absolutePath) } }
                pendingFile = null
                throw cancelled
            } catch (error: Exception) {
                withContext(Dispatchers.IO) { prepared?.let { GraphyExportFiles.discard(context, it.absolutePath) } }
                pendingFile = null
                failed(error)
            } finally { busy = false }
        }
    }
}

@Composable
internal fun GraphyDownloadButton(state: GraphyDownloadState) {
    var expanded by remember { mutableStateOf(false) }
    val preparing = stringResource(R.string.graphy_export_preparing)
    Box {
        IconButton(onClick = { expanded = true }, enabled = state.enabled) {
            if (state.busy) CircularProgressIndicator(Modifier.size(dimensionResource(R.dimen.graphy_spacing_lg)).semantics { contentDescription = preparing },
                strokeWidth = dimensionResource(R.dimen.graphy_node_border_stroke))
            else Icon(painterResource(R.drawable.ic_graphy_download), stringResource(R.string.graphy_download))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.graphy_export_pdf)) }, onClick = { expanded = false; state.prepare(Format.PDF) })
            DropdownMenuItem(text = { Text(stringResource(R.string.graphy_export_png)) }, onClick = { expanded = false; state.prepare(Format.PNG) })
        }
    }
}
