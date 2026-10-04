package net.smartlogic.unitconverter.theme

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyTheme
import net.smartlogic.unitconverter.helper.Preferences

/** Compose interoperability follows the existing Graphy and timer hosts. */
object ThemeSelectorController {
    @JvmStatic
    fun bind(view: ComposeView, fragment: Fragment, onSelected: Runnable) {
        val viewModel = ViewModelProvider(
            fragment, ThemeSelectorViewModel.Factory(Preferences.getInstance(fragment.requireContext())),
        )[ThemeSelectorViewModel::class.java]
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            GraphyTheme {
                ThemeSelector(
                    state = state,
                    onThemeTapped = { id ->
                        if (viewModel.requestTheme(id) && id != ThemeManager.get().id()) {
                            ThemeManager.select(view.context, id)
                            onSelected.run()
                        }
                    },
                    onDismissDialog = viewModel::dismissLockedTheme,
                )
            }
        }
    }
}
