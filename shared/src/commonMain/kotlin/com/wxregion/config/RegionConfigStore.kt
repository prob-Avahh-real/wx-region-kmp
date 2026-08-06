package com.wxregion.config

import com.wxregion.model.RegionConfig
import com.wxregion.model.RegionMode
import com.wxregion.model.ApplyMethod

/** 跨平台配置读写 */
expect class RegionConfigStore() {
    fun load(): RegionConfig
    fun save(config: RegionConfig)
    fun syncToLsposedModule(mode: RegionMode)
}

expect fun defaultApplyMethod(): ApplyMethod
