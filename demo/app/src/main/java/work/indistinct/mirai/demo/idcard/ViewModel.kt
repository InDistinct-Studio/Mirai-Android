package work.indistinct.mirai.demo.idcard

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import work.indistinct.mirai.IDCardResult

data class CaptureCardViewState(
    val result: IDCardResult? = null,
)

class CaptureCardViewModel : ViewModel() {

    val viewState: MutableState<CaptureCardViewState> = mutableStateOf(CaptureCardViewState())

    fun resetResult() {
        viewState.value = viewState.value.copy(result = null)
    }

    fun setCaptureResult(result: IDCardResult) {
        viewState.value = viewState.value.copy(result = result)
    }

}

