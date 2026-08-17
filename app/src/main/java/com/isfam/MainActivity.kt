package com.isfam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.navigation.IsFamNavHost

/**
 * 앱의 유일한 Activity.
 *
 * Compose + Navigation 구조에서는 화면마다 Activity 를 만들지 않습니다.
 * Activity 하나 안에서 NavHost 가 화면을 갈아 끼웁니다.
 * (예전 Activity/Fragment 방식과 가장 크게 다른 점입니다)
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            IsFamTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        // 상태바·네비게이션바 영역을 침범하지 않도록
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .consumeWindowInsets(WindowInsets.systemBars),
                ) {
                    IsFamNavHost()
                }
            }
        }
    }
}