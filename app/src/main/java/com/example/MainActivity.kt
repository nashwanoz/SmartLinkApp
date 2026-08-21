package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LoginScreen
import com.example.ui.LoginViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val loginViewModel: LoginViewModel = viewModel()
      val uiState = loginViewModel.uiState.collectAsStateWithLifecycle().value

      MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
          LoginScreen(viewModel = loginViewModel)
        }
      }
    }
  }
}

