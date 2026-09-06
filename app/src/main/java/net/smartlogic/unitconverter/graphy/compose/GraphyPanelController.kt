package net.smartlogic.unitconverter.graphy.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyTheme
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeDefaults
import net.smartlogic.unitconverter.graphy.compose.ui.GraphyPanel
import net.smartlogic.unitconverter.graphy.model.GraphyOutput
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme

/**
 * Java-friendly bridge for hosting Graphy Compose UI inside existing View/XML screens.
 */
class GraphyPanelController(
    private val composeView: ComposeView,
    private val fragment: Fragment,
    private val viewTheme: GraphyViewTheme,
) {
    private var explanation by mutableStateOf("")
    private var output by mutableStateOf<GraphyOutput?>(null)

    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
        )
        composeView.setContent {
            val darkTheme = GraphyThemeDefaults.isDarkTheme(composeView.context)
            GraphyTheme(darkTheme = darkTheme) {
                GraphyPanel(
                    explanation = explanation,
                    output = output,
                    viewTheme = viewTheme,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    fun update(explanation: String, output: GraphyOutput?) {
        this.explanation = explanation
        this.output = output
    }
}
