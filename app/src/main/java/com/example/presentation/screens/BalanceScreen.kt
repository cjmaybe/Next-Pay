package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ModeRouter
import com.example.domain.SessionState
import com.example.presentation.BalanceViewModel
import com.example.presentation.ui.components.OfflineStatusHeader
import com.example.presentation.ui.components.PaymentResultCard
import com.example.presentation.ui.components.PinInputDialog

@Composable
fun BalanceScreen(
    viewModel: BalanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        val activeCarrierName = uiState.availableSims.find { it.slotIndex == uiState.selectedSimSlot }?.carrierName ?: "SIM 1"
        OfflineStatusHeader(
            activeCarrier = activeCarrierName,
            isAccessibilityEnabled = uiState.isAccessibilityEnabled || uiState.isManualMode,
            onEnableAccessibilityClick = { ModeRouter.openAccessibilitySettings(context) }
        )

        // Session State Result Card
        if (sessionState !is SessionState.Idle) {
            PaymentResultCard(
                state = sessionState,
                onDismiss = { viewModel.resetSessionState() }
            )
        }

        // Primary Balance Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("balance_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Bank Balance",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Bank Account Balance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))
                val balanceDisplay = if (uiState.lastBalance.isNotBlank()) "₹${uiState.lastBalance}" else "₹ --.--"
                Text(
                    text = balanceDisplay,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (uiState.lastBalanceAccount.isNotBlank()) {
                    Text(
                        text = uiState.lastBalanceAccount,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = "Last updated: ${uiState.lastCheckedFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(20.dp))

                // SIM Card Picker
                if (uiState.availableSims.size > 1) {
                    Text(
                        text = "Choose Bank Registered SIM",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (sim in uiState.availableSims) {
                            FilterChip(
                                selected = sim.slotIndex == uiState.selectedSimSlot,
                                onClick = { viewModel.onSimSlotSelected(sim.slotIndex) },
                                label = { Text("SIM ${sim.slotIndex}: ${sim.carrierName}") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.SimCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Check Balance Button
                Button(
                    onClick = { viewModel.onCheckBalanceClicked() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("check_balance_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Check Balance (*99#)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { viewModel.onTestSimulationClicked() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_balance_autofill_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Test Balance Auto-Fill (Live Simulation)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // How it works card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Direct Bank-Grade Security",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "• *99# connects directly to your bank account via encrypted GSM USSD.\n• Your UPI PIN is entered securely and never saved on your phone.\n• Completely free from data costs and works in zero-internet zones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }

    // PIN Dialog
    if (uiState.showPinDialog) {
        PinInputDialog(
            title = "Enter UPI PIN",
            subtitle = "Required by bank to check balance on SIM ${uiState.selectedSimSlot}",
            pinLength = 6,
            onPinConfirmed = { pin -> viewModel.onPinConfirmed(pin) },
            onDismiss = { viewModel.onPinDialogDismissed() }
        )
    }
}
