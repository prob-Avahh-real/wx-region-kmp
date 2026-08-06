package com.wxregion

import com.wxregion.model.RegionMode
import kotlin.test.Test
import kotlin.test.assertEquals

class RegionModeTest {
    @Test
    fun hiddenHasNoDisplayText() {
        assertEquals(null, RegionMode.HIDDEN.displayText)
    }

    @Test
    fun mainlandChinaLabel() {
        assertEquals("中国大陆", RegionMode.MAINLAND_CHINA.displayText)
    }
}
