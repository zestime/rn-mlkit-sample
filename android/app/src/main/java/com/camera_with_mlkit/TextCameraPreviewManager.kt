package com.camera_with_mlkit

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class TextCameraPreviewManager(val reactContext: ReactApplicationContext) : SimpleViewManager<TextCameraPreview>() {
    private val REACT_CLASS = "EduTextCameraPreview"

    override fun createViewInstance(context: ThemedReactContext): TextCameraPreview {
        val activity = reactContext.currentActivity
        return TextCameraPreview(context, activity)
    }

    override fun getName(): String {
        return REACT_CLASS
    }

    @ReactProp(name="permission")
    fun setPermission(preview: TextCameraPreview, hasPermission:Boolean) {
        Log.d("TextCameraPreview", "setPermission: $hasPermission")
        if(hasPermission)
            preview.startCamera()
    }

    override fun getExportedCustomBubblingEventTypeConstants(): Map<String, Any> {
        return MapBuilder.builder<String,Any>().put("recognized", MapBuilder.of(  "phasedRegistrationNames",
            MapBuilder.of("bubbled", "onRecognize"))).build()

    }
}