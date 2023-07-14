package work.indistinct.mirai.demo.home

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavController
import work.indistinct.mirai.demo.common.AppBar

@Composable
fun HomeScreen(
    viewState: HomeViewState,
    initMiraiSDK: (Context) -> Unit,
    navController: NavController,
) {
    val context = LocalContext.current
    initMiraiSDK(context)

    Scaffold(topBar = { AppBar(title = "Mirai Demo", navController) }) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column {
                when (viewState.status) {
                    HomeViewState.MiraiStatus.Ready -> {
                        MainMenu(onClick = { mode ->

                        })
                    }
                    is HomeViewState.MiraiStatus.Error -> {
                        Text("Error occurred: ${viewState.status.errorMessage}")
                    }
                    HomeViewState.MiraiStatus.InProgress -> {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun MainMenu(onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .padding(Dp(4f))
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = {
                onClick("OCR")
            },
        ) {
            Text(text = "OCR")
        }
    }
}