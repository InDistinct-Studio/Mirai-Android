package work.indistinct.mirai.demo.idcard

import android.Manifest
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onStartAnalyzeImage: () -> Unit,
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

            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            if (permissionState.status.isGranted) {
                CameraView(overlay = {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val screenWidth =
                            with(density) { configuration.screenWidthDp.dp.roundToPx() }
                        val screenHeight =
                            with(density) { configuration.screenHeightDp.dp.roundToPx() }

                        val x = screenWidth * 0.1f
                        val y = screenHeight * 0.15f

                        drawRect(Color.Black)
                        drawRect(
                            topLeft = Offset(x, y),
                            size = Size(screenWidth * 0.8f, screenHeight * 0.6f),
                            color = Color.Transparent,
                            blendMode = BlendMode.Clear
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(all = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "ถ่ายรูปบัตรประชาชน",
                            style = TextStyle(fontSize = 20.sp, color = Color.White)
                        )
                        Text(
                            "หมุนบัตรประชาชนให้อยู่ในแนวตั้ง และถ่ายรูปบัตร โดยไม่ให้มีสิ่งบดบัง รวมถึงแสงสะท้อนบนหน้าบัตร",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (!viewState.isCapturing) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onStartAnalyzeImage()
                                }) {
                                Text("เริ่มต้นการสแกน")
                            }
                        }
                    }
                }, analyzeImage = { imageProxy ->
                    if (viewState.isCapturing) {
                        analyzeImage(imageProxy, onCardCaptured)
                    }
                })
            }
        } else {
            CardInfo(
                result = viewState.result,
                navController = navController,
                onUseResetCaptureResult
            )
        }
    }
}

@Composable
fun CardInfo(result: IDCardResult, navController: NavController, onUserTapReTake: () -> Unit) {
    Column(
        modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.fillMaxWidth(),
            bitmap = result.croppedImage!!.asImageBitmap(),
            contentDescription = "",
            contentScale = ContentScale.FillWidth
        )
        Spacer(modifier = Modifier.height(height = 16.dp))
        result.texts!!.forEach {
            Text(
                "${it.type.name}: ${it.text}",
                style = TextStyle(
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                navController.popBackStack()
            }) {
                Text("Confirm")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = {
                onUserTapReTake()
            }) {
                Text("Re-take")
            }
        }
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
            if (result.confidence > 0.8f && (result.detectResult?.mlConfidence ?: 0f) > 0.9f) {
                onCardCaptured(result)
            }
            imageProxy.close()
        }
    }
}