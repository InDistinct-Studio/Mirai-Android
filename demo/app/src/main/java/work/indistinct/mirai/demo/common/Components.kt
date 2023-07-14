package work.indistinct.mirai.demo.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    title: String,
    navController: NavController,
) {
    val navIcon = @Composable {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, "backIcon")
        }
    }

    (if (navController.currentDestination?.route != "/") navIcon else null)?.let {
        TopAppBar(
        title = { Text(title) },
        navigationIcon = it
    )
    }
}

@Preview
@Composable
fun AppbarPreview() {
    AppBar("TESTER", rememberNavController())
}
