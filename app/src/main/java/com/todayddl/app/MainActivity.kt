package com.todayddl.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.todayddl.app.ui.navigation.TodayDdlNavHost
import com.todayddl.app.ui.theme.TodayDdlTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodayDdlTheme {
                TodayDdlNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as TodayDdlApplication
        lifecycleScope.launch(Dispatchers.IO) {
            app.container.repository.refreshOngoingAndMidnightOnly()
        }
    }
}
