package work.indistinct.mirai.demo.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import work.indistinct.mirai.Mirai

class HomeViewModel : ViewModel(), Mirai.OnInitializedListener {

    val viewState: MutableState<HomeViewState> = mutableStateOf(HomeViewState())

    override fun onCompleted() {
        viewState.value = viewState.value.copy(status = HomeViewState.MiraiStatus.Ready)
    }

    override fun onError(message: String) {
        viewState.value = viewState.value.copy(status = HomeViewState.MiraiStatus.Error(message))
    }
}
data class HomeViewState(
    val status: MiraiStatus = MiraiStatus.InProgress
) {
    sealed interface MiraiStatus {
        object Ready : MiraiStatus
        object InProgress : MiraiStatus
        data class Error(val errorMessage: String) : MiraiStatus
    }
}