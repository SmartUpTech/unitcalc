package net.smartlogic.unitconverter.timer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyTheme
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeDefaults
import net.smartlogic.unitconverter.timer.compose.TimerScreen

class TimerFragment : Fragment() {
    private lateinit var viewModel: TimerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val engine = TimerEngine.getInstance(requireContext())
        viewModel = ViewModelProvider(
            this,
            TimerViewModelFactory(engine),
        )[TimerViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val darkTheme = GraphyThemeDefaults.isDarkTheme(requireContext())
                GraphyTheme(darkTheme = darkTheme) {
                    TimerScreen(viewModel = viewModel)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): TimerFragment = TimerFragment()
    }
}
