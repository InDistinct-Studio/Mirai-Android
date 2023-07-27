package work.indistinct.mirai.demo.idcard

import android.Manifest
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import work.indistinct.mirai.CardImage
import work.indistinct.mirai.IDCardResult
import work.indistinct.mirai.Mirai
import work.indistinct.mirai.demo.common.CameraView

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureCardScreen(
    navController: NavController,
    onUseResetCaptureResult: () -> Unit,
    onCardCaptured: (IDCardResult) -> Unit,
    viewState: CaptureCardViewState,
) {
    Scaffold {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
        )


        if (viewState.result == null) {
            val permissionState = rememberPermissionState(
                permission = Manifest.permission.CAMERA
            )
            LaunchedEffect(Unit) {
                permissionState.launchPermissionRequest()
            }

            if (permissionState.status.isGranted) {
                CameraView(analyzeImage = { imageProxy ->
                    analyzeImage(imageProxy, onCardCaptured)
                })
            }
        } else {
            CardInfo(result = viewState.result)
        }
    }
}

@Composable
fun CardInfo(result: IDCardResult) {
    Column {
        Image(bitmap = result.croppedImage!!.asImageBitmap(), contentDescription = "")
        Spacer(modifier = Modifier.height(height = 16.dp))
        Text("ID: ${result.texts!!.first { it.type == IDCardResult.Type.ID }}")
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun analyzeImage(imageProxy: ImageProxy, onCardCaptured: (IDCardResult) -> Unit) {
    imageProxy.run {
        val reverseDimens = imageInfo.rotationDegrees == 90 || imageInfo.rotationDegrees == 270
        // TODO: Consider using this later
//        val imgProxyWidth = if (reverseDimens) imageProxy.height else imageProxy.width
//        val imgProxyHeight = if (reverseDimens) imageProxy.width else imageProxy.height
//        val imgProxyRotationDegree = imageInfo.rotationDegrees
        val card = CardImage(this.image!!, imageInfo.rotationDegrees)
        Mirai.scanIDCard(card) { result ->
            if (result.confidence > 0.6f) {
                onCardCaptured(result)
            }
            imageProxy.close()
        }
    }
}