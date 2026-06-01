package com.mohammed.pdfreader

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.mohammed.pdfreader.ui.navigation.AppNavigation
import com.mohammed.pdfreader.ui.theme.PDFReaderTheme
import com.mohammed.pdfreader.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle PDF intent from other apps
        val intentUri: Uri? = when (intent?.action) {
            android.content.Intent.ACTION_VIEW -> intent.data
            else -> null
        }

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            PDFReaderTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        intentUri = intentUri,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
