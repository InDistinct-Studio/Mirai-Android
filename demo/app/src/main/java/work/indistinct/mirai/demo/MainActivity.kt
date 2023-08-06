package work.indistinct.mirai.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import work.indistinct.mirai.Mirai
import work.indistinct.mirai.demo.home.HomeScreen
import work.indistinct.mirai.demo.home.HomeViewModel
import work.indistinct.mirai.demo.idcard.CaptureCardScreen
import work.indistinct.mirai.demo.idcard.CaptureCardViewModel

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
                    initMiraiSDK = { context -> Mirai.init(context, "NDIfT5TihAj7wVZi178O", viewModel) },
                    viewState = viewModel.viewState.value
                )
            }
            navigation(route = "idCard", startDestination = "captureCard") {
                composable("captureCard") {
                    val viewModel = viewModel<CaptureCardViewModel>()
                    CaptureCardScreen(navController,
                        viewModel::startCapture,
                        viewModel::resetResult,
                        viewModel::setCaptureResult,
                        viewModel.viewState.value,
                    )
                }
                composable("detectLiveness") {

                }
            }
        }
    }

}