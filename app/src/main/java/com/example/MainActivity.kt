package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.presentation.navigation.MainScaffold
import com.example.presentation.screens.onboarding.OnboardingFlow
import com.example.ui.theme.NextPayTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NextPayTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val prefs = NextPayApplication.instance.preferencesRepository
                    val onboardingCompleted by prefs.isOnboardingCompleted.collectAsState(initial = true)
                    val scope = rememberCoroutineScope()

                    if (!onboardingCompleted) {
                        OnboardingFlow(
                            onComplete = {
                                scope.launch {
                                    prefs.setOnboardingCompleted(true)
                                }
                            }
                        )
                    } else {
                        MainScaffold()
                    }
                }
            }
        }
    }
}
