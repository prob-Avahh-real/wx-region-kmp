package com.wxregion.config

import com.wxregion.model.ApplyMethod
import com.wxregion.model.RegionConfig
import com.wxregion.model.RegionMode

actual class RegionConfigStore actual constructor() {
    private var cached = RegionConfig()

    actual fun load(): RegionConfig = cached

    actual fun save(config: RegionConfig) {
        cached = config
    }

    actual fun syncToLsposedModule(mode: RegionMode) = Unit
}

actual fun defaultApplyMethod(): ApplyMethod = ApplyMethod.ACCESSIBILITY
