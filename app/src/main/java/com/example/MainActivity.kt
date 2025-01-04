package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.VoiceChatScreen
import com.example.ui.VoiceChatViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: VoiceChatViewModel by viewModels {
        VoiceChatViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkMode by viewModel.darkMode.collectAsState(initial = false)
            MyApplicationTheme(darkTheme = darkMode, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    VoiceChatScreen(viewModel = viewModel)
                }
            }
        }
    }
}
