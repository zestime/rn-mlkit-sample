package com.camera_with_mlkit

import android.view.View
import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ReactShadowNode
import com.facebook.react.uimanager.ViewManager

typealias RCTView =  ViewManager<View, ReactShadowNode<*>>

class CalendarPackage():ReactPackage{
    override fun createNativeModules(reactContext: ReactApplicationContext): MutableList<NativeModule> {
        return mutableListOf(CalendarModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): MutableList<RCTView> {
        return mutableListOf(
            TextCameraPreviewManager(reactContext) as RCTView,
            ImageViewManager(reactContext) as RCTView
        );
    }

//    override fun createViewManagers(reactContext: ReactApplicationContext): MutableList<ViewManager> {
//        return listOf<ViewManager>(CalendarModule(reactContext));
//    }

}