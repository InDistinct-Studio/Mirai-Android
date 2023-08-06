package work.indistinct.mirai.demo.idcard

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import work.indistinct.mirai.IDCardResult

data class CaptureCardViewState(
    val isProcessing: Boolean = false,
    val isCapturing: Boolean = false,
    val result: IDCardResult? = null,
)

class CaptureCardViewModel : ViewModel() {

    val viewState: MutableState<CaptureCardViewState> = mutableStateOf(CaptureCardViewState())

    fun startCapture() {
        viewState.value = viewState.value.copy(isCapturing = true)
    }

    fun resetResult() {
        viewState.value = viewState.value.copy(result = null)
    }

    fun setCaptureResult(result: IDCardResult) {
        viewModelScope.launch {
            viewState.value = viewState.value.copy(result = result,
                isCapturing = false, isProcessing = true)
            //TODO: Connect with API
            viewState.value = viewState.value.copy(isProcessing = false)
        }

    }

}

