[![](https://jitpack.io/v/InDistinct-Studio/Mirai-Android.svg)](https://jitpack.io/#InDistinct-Studio/Mirai-Android)


# Mirai OCR Library

## Prerequisite

- Android SDK Version 24 or Higher
- Google Play Services
- `Internet` and `Camera` Permissions in Manfiest

## Installation Guide

- Add this to your `build.gradle` at the root of the project
  ```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  ```
- Add your dependency in your app or feature module as below
```
	dependencies {
		implementation 'com.github.InDistinct-Studio:mirai-android:<version>'
	}
```

- Sync your gradle project and Mirai is now available for your application

## HOW-TO

### Card Detection

1. Initialize SDK by calling `init` function with your API Key. Also, you will also need to implement `Mirai.OnInitializedListener` to check whether the initialization is completed or not

    ```kotlin
    Mirai.init(this, "API_KEY", listener)
    ```


1. Create the `CardImage` object which will be used as an input for OCR

    ```kotlin
    val card = CardImage(this.image!!, imageInfo.rotationDegrees)
    ```

2. Call the `scanIDCard` method from `Mirai` object class which will extract card information from the image. There are 3 required parameters.
    - `card`: Input card image
    - `resultListener`: The listener to receive recognition result. This will provide the information of cards that can be detected in `IDCardResult` object

      ```kotlin
      imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
              imageProxy.run {
                val card = CardImage(this.image!!, imageInfo.rotationDegrees)
                Mirai.scanIDCard(card) {result ->
                  this@MainActivity.displayResult(result)
                  imageProxy.close()
                }
              }
            }
       ```

3. Use the resulting `IDCardResult` object will consist of the following fields:
    - `error`: If the recognition is successful, the `error` will be null. In case of unsuccessful scan, the `error.errorMessage` will contain the problem of the recognition.
    - `isFrontSide`: A boolean flag indicates whether the scan found the front side (`true`) or back side (`false`) of the card ID.
    - `confidence`: A value between 0.0 to 1.0 (higher values mean more likely to be an ID card).
    - `isFrontCardFull`: A boolean flag indicates whether the scan detect a full front-sde card.
    - `texts`: A list of OCR results. An OCR result consists of `type` and `text`.
        - `type`: Type of information. Right now, Mirai support 3 types: `ID`, `SERIAL_NUMBER`, and `LASER_CODE`
        - `text`: OCR text based on the `type`.
    - `fullImage`: A bitmap image of the full frame used during scanning.
    - `croppedImage`: A bitmap image of the card. This is available if `isFrontSide` is `true`.
    - `classificationResult`: A result from ML image labeling, available if `isFrontCardFull` is `true`.
      - `confidence`: A confidence value from 0 to 1. Higher values mean the images are more likely to be good quality. The threshold of `0.6` to `0.9` is recommended. 
      - `error`: An object for error messages.

### Face Action Screening

1. Same as Card Detection setup, you need to cal `init` function with your API Key but you will need to specify "face-actions" option to enable this feature.

```kotlin
    Mirai.init(this, "API_KEY", options = listOf("face-actions"), listener)
```

2. Before you can use Face Actions Screening, you need to call `initFaceScreeningState` to initialize the whole process.

3. To use face screening, you just need to call `twoStageCheckFace` function. This function will return `FaceScreeningState` object which will show all the state of screening state. Here are some parameters that you should know
    - `stage` - represent the current stage of screening process. Here are all possible stages
        - `FaceScreeningStage.FRONT` - The stage to check for staright face
        - `FaceScreeningStage.LEFT` - The stage to check the turning left face
        - `FaceScreeningStage.RIGHT` - The stage to check the turning left right
        - `FaceScreeningStage.UP` - The stage to check the turning face up
        - `FaceScreeningStage.DOWN` - The stage to check the turning face down
        - `FaceScreeningStage.BLINK` - The stage to check for blinking 
        - `FaceScreeningStage.MOUTHOPEN` - The stage to check face with mouth opening
        - `FaceScreeningStage.FINISH` - The stage that concluded the process and screening successfully
        - `FaceScreeningStage.FAILED` - The stage that indicate that face screening is failed.
    - `results` - list of `FaceScreeningResult` which contain the captured face data. Here are information about face that you can use.
        - `curFaceDetectionResult` - The result of face detection image. This will contain data related to captured face image e.g. image size, face rotation, and etc.

    The process will start screening in 2 steps. First, it will try to find the user's straight face. This is to setup and calibrate the alogrithm for action screening. This will start automatically when you start calling this function.

4. After first step is done, SDK will update `stage` parameter from `FaceScreeningStage.FRONT` to stage with input action (`expectedAction`). For example, If you set `expectedAction` as blink action, it will return `FaceScreeningStage.BLINK` after the first stage is done. To start verify the next action, you need to call `initFaceScreeningSecondStage`. 

5. After the process is done, the `stage` will change `FaceScreeningStage.FINISH` and you can get all the face results from `results` parameter.