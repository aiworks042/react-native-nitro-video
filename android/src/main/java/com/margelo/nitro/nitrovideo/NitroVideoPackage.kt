package com.margelo.nitro.nitrovideo

import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.BaseReactPackage
import com.facebook.react.uimanager.ViewManager
import com.margelo.nitro.nitrovideo.views.HybridNitroVideoViewManager

class NitroVideoPackage : BaseReactPackage() {
    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? = null

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider = ReactModuleInfoProvider { HashMap() }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        val viewManagers = ArrayList<ViewManager<*, *>>()
        viewManagers.add(HybridNitroVideoViewManager())
        return viewManagers
    }

    companion object {
        init {
            NitroVideoOnLoad.initializeNative()
        }
    }
}
