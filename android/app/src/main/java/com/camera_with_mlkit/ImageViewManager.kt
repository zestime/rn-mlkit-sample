package com.camera_with_mlkit

import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewProps
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.views.image.ImageResizeMode
import com.facebook.react.views.image.ReactImageView

class ImageViewManager(val reactContext: ReactApplicationContext) : SimpleViewManager<ReactImageView>() {
    private val REACT_CLASS = "KHImageView"

    override fun createViewInstance(context: ThemedReactContext): ReactImageView {
        return ReactImageView(context, Fresco.newDraweeControllerBuilder(), null, reactContext )
    }

    override fun getName(): String {
        return REACT_CLASS
    }

    @ReactProp(name="src")
    fun setSrc(view: ReactImageView, sources: ReadableArray?) {
        view.setSource(sources)
    }

    @ReactProp(name = "borderRadius", defaultFloat =  0f)
    fun setViewBorderRadius(view: ReactImageView, borderRadius:Float){
        view.setBorderRadius(borderRadius)
    }

    @ReactProp(name = ViewProps.RESIZE_MODE)
    fun setResizeMode(view:ReactImageView, resizeMode:String?) {
        view.setScaleType(ImageResizeMode.toScaleType(resizeMode))
    }
}