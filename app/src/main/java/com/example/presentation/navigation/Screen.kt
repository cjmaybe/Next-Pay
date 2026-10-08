package com.example.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Pay : Screen(
        route = "pay",
        title = "Pay",
        selectedIcon = Icons.Filled.Send,
        unselectedIcon = Icons.Outlined.Send
    )

    data object Scan : Screen(
        route = "scan",
        title = "Scan QR",
        selectedIcon = Icons.Filled.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner
    )

    data object Balance : Screen(
        route = "balance",
        title = "Balance",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet
    )

    data object History : Screen(
        route = "history",
        title = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object Faq : Screen(
        route = "faq",
        title = "Help & FAQ",
        selectedIcon = Icons.Filled.HelpOutline,
        unselectedIcon = Icons.Filled.HelpOutline
    )
}
