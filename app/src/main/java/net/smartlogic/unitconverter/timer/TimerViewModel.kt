package net.smartlogic.unitconverter.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.smartlogic.unitconverter.timer.model.RecentTimer
import net.smartlogic.unitconverter.timer.model.TimerDraft

class TimerViewModel(
    private val engine: TimerEngine,
) : ViewModel() {
    val uiState: StateFlow<net.smartlogic.unitconverter.timer.model.TimerUiState> =
        engine.uiState.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            engine.uiState.value,
        )

    init {
        viewModelScope.launch {
            while (isActive) {
                engine.refreshDisplay()
                delay(250L)
            }
        }
    }

    fun updateDraft(draft: TimerDraft) = engine.updateDraft(draft)
    fun startFromDraft() = engine.startFromDraft()
    fun startRecent(recent: RecentTimer) = engine.startRecent(recent)
    fun confirmReplaceRecent() = engine.confirmReplaceRecent()
    fun dismissReplaceRecent() = engine.dismissReplaceRecent()
    fun pause() = engine.pause()
    fun resume() = engine.resume()
    fun restart() = engine.restart()
    fun reset() = engine.reset()
    fun dismissCompleted() = engine.dismissCompleted()
}

class TimerViewModelFactory(
    private val engine: TimerEngine,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
            return TimerViewModel(engine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
