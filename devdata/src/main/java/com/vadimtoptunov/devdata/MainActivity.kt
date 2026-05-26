package com.vadimtoptunov.devdata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vadimtoptunov.devdata.ui.DevDataApp
import com.vadimtoptunov.devdata.ui.theme.DevDataTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DevDataTheme {
                DevDataApp()
            }
        }
    }
}
