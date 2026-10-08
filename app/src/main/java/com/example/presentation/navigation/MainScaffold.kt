package com.example.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.BalanceViewModel
import com.example.presentation.HistoryViewModel
import com.example.presentation.PayViewModel
import com.example.presentation.screens.BalanceScreen
import com.example.presentation.screens.FaqScreen
import com.example.presentation.screens.HistoryScreen
import com.example.presentation.screens.PayScreen
import com.example.presentation.screens.ScanScreen
import com.example.presentation.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Pay) }
    var previousScreen by remember { mutableStateOf<Screen?>(null) }

    val payViewModel: PayViewModel = viewModel()
    val balanceViewModel: BalanceViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()

    // Handle back button when in a sub-screen
    BackHandler(enabled = currentScreen != Screen.Pay) {
        currentScreen = previousScreen ?: Screen.Pay
    }

    val bottomNavScreens = remember {
        listOf(Screen.Pay, Screen.Scan, Screen.Balance, Screen.History)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            Screen.Pay -> "Next Pay"
                            Screen.Scan -> "Scan UPI QR"
                            Screen.Balance -> "Bank Balance"
                            Screen.History -> "Transactions"
                            Screen.Settings -> "Settings"
                            Screen.Faq -> "Help & FAQ"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (currentScreen == Screen.Settings || currentScreen == Screen.Faq) {
                        IconButton(onClick = { currentScreen = previousScreen ?: Screen.Pay }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        IconButton(onClick = { /* Info */ }) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Offline Enabled",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (currentScreen != Screen.Faq) {
                        IconButton(onClick = {
                            previousScreen = currentScreen
                            currentScreen = Screen.Faq
                        }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "FAQ")
                        }
                    }
                    if (currentScreen != Screen.Settings) {
                        IconButton(onClick = {
                            previousScreen = currentScreen
                            currentScreen = Screen.Settings
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (currentScreen in bottomNavScreens) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    bottomNavScreens.forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                previousScreen = currentScreen
                                currentScreen = screen
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                Screen.Pay -> {
                    PayScreen(
                        viewModel = payViewModel,
                        onNavigateToScan = {
                            previousScreen = currentScreen
                            currentScreen = Screen.Scan
                        }
                    )
                }
                Screen.Scan -> {
                    ScanScreen(
                        onQrScanned = { upiData ->
                            payViewModel.setFromQr(upiData)
                            currentScreen = Screen.Pay
                        }
                    )
                }
                Screen.Balance -> {
                    BalanceScreen(
                        viewModel = balanceViewModel
                    )
                }
                Screen.History -> {
                    HistoryScreen(
                        viewModel = historyViewModel
                    )
                }
                Screen.Settings -> {
                    SettingsScreen(
                        onNavigateToFaq = {
                            previousScreen = Screen.Settings
                            currentScreen = Screen.Faq
                        }
                    )
                }
                Screen.Faq -> {
                    FaqScreen()
                }
            }
        }
    }
}
