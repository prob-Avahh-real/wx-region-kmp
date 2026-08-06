package com.wxregion

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.wxregion.ui.HomeScreen
import com.wxregion.ui.theme.WxRegionTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    WxRegionTheme {
        HomeScreen()
    }
}
