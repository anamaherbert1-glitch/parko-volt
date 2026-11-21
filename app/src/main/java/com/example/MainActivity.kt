package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ParkoDatabase
import com.example.data.local.SessionPreferences
import com.example.data.model.ThemeMode
import com.example.data.repository.ParkoRepository
import com.example.ui.ParkoApp
import com.example.ui.theme.ParkoTheme
import com.example.ui.viewmodel.ParkoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = ParkoDatabase.getInstance(applicationContext)
        val prefs = SessionPreferences(applicationContext)
        val repository = ParkoRepository(db.parkoDao(), prefs)

        setContent {
            val parkoViewModel: ParkoViewModel = viewModel(
                factory = ParkoViewModel.Factory(repository)
            )
            val themeMode by parkoViewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            ParkoTheme(darkTheme = darkTheme) {
                ParkoApp(viewModel = parkoViewModel)
            }
        }
    }
}
