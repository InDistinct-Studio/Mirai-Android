package work.indistinct.mirai.demo

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import work.indistinct.mirai.Mirai
import work.indistinct.mirai.demo.home.HomeScreen
import work.indistinct.mirai.demo.home.HomeViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppNavHost(navController = rememberNavController())
        }
    }

    @Composable
    fun AppNavHost(navController: NavHostController) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                val viewModel = viewModel<HomeViewModel>()
                HomeScreen(
                    navController = navController,
                    initMiraiSDK = { context -> Mirai.init(context, "j5gSKKFd1hl02ctNVE2l", viewModel) },
                    viewState = viewModel.viewState.value
                )
            }
        }
    }

}