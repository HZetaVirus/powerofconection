package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.MainApp
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  
  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Global Uncaught Exception Handler to locate any thread-level crashes
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      android.util.Log.e("PowerConnectionCrash", "CRITICAL UNCAUGHT EXCEPTION on thread '${thread.name}': ${throwable.message}", throwable)
      if (defaultHandler != null) {
        defaultHandler.uncaughtException(thread, throwable)
      } else {
        android.os.Process.killProcess(android.os.Process.myPid())
        java.lang.System.exit(10)
      }
    }

    enableEdgeToEdge()
    setContent {
      // Create a rememberable mutable dark theme state that is toggleable
      val darkThemeState = remember { mutableStateOf(false) }

      MyApplicationTheme(darkTheme = darkThemeState.value) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainApp(
            viewModel = viewModel,
            darkThemeState = darkThemeState
          )
        }
      }
    }
  }
}
