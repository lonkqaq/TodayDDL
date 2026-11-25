package com.todayddl.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.todayddl.app.ui.navigation.TodayDdlNavHost
import com.todayddl.app.ui.theme.TodayDdlTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TodayDdlApplication
        app.appScope.launch {
            app.container.repository.initializeDefaults()
        }
        setContent {
            TodayDdlTheme {
                TodayDdlNavHost()
            }
        }
    }
}
