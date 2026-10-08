package com.example.presentation.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ModeRouter
import com.example.domain.SessionState
import com.example.presentation.PayViewModel
import com.example.presentation.ui.components.OfflineStatusHeader
import com.example.presentation.ui.components.PaymentResultCard
import com.example.presentation.ui.components.PinInputDialog

@Composable
fun PayScreen(
    viewModel: PayViewModel,
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshSimsAndAccessibility()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Status header & accessibility warning
        val activeCarrierName = uiState.availableSims.find { it.slotIndex == uiState.selectedSimSlot }?.carrierName ?: "SIM 1"
        OfflineStatusHeader(
            activeCarrier = activeCarrierName,
            isAccessibilityEnabled = uiState.isAccessibilityEnabled || uiState.isManualMode,
            onEnableAccessibilityClick = {
                ModeRouter.openAccessibilitySettings(context)
            }
        )

        // If a session has result or in-progress, show card
        if (sessionState !is SessionState.Idle) {
            PaymentResultCard(
                state = sessionState,
                onDismiss = { viewModel.resetSessionState() },
                onShareReceipt = { receiptText ->
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Next Pay Transaction Receipt")
                        putExtra(Intent.EXTRA_TEXT, receiptText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
                }
            )
        }

        // Primary Pay Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("pay_form_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Money",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline Transfer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Direct via NPCI *99# USSD",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Scan QR button shortcut
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onNavigateToScan() }
                            .testTag("scan_qr_shortcut_button"),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Scan",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Recipient Input
                OutlinedTextField(
                    value = uiState.recipient,
                    onValueChange = { viewModel.onRecipientChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recipient_input"),
                    label = { Text("UPI ID or 10-Digit Mobile") },
                    placeholder = { Text("e.g. 9876543210 or name@bank") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = { viewModel.pasteFromClipboard(context) },
                            modifier = Modifier.testTag("paste_recipient_button")
                        ) {
                            Text("Paste", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    isError = uiState.recipientError != null,
                    supportingText = {
                        if (uiState.recipientError != null) {
                            Text(text = uiState.recipientError!!)
                        } else if (uiState.recipientName.isNotBlank()) {
                            Text(text = "Payee: ${uiState.recipientName}")
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Amount Input
                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = { viewModel.onAmountChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input"),
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CurrencyRupee,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.amountError != null,
                    supportingText = {
                        if (uiState.amountError != null) {
                            Text(text = uiState.amountError!!)
                        } else {
                            Text("Limit ₹1 - ₹5,000 per offline transaction")
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Quick Amount Chips
                Text(
                    text = "Quick Amounts",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickAmounts = listOf(50, 100, 200, 500, 1000, 2000)
                    for (amt in quickAmounts) {
                        FilterChip(
                            selected = uiState.amount == amt.toString(),
                            onClick = { viewModel.setQuickAmount(amt) },
                            label = { Text("₹$amt") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // SIM Selection (Dual SIM support)
                if (uiState.availableSims.size > 1) {
                    Column {
                        Text(
                            text = "Select SIM Card for *99# USSD",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (sim in uiState.availableSims) {
                                val isSelected = sim.slotIndex == uiState.selectedSimSlot
                                FilterChip(
                                    selected = isSelected,
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
                    }
                }

                // Optional Note
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = { viewModel.onNoteChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note / Remark (Optional)") },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Primary Action Button
                Button(
                    onClick = { viewModel.onProceedToPayClicked() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("send_money_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.amount.isNotBlank()) "Send ₹${uiState.amount} Offline" else "Send Money",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Interactive Auto-Fill Test Mode Button (Verifies the full automation workflow)
                OutlinedButton(
                    onClick = { viewModel.onTestSimulationClicked() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_autofill_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Test USSD Auto-Fill (Live Simulation)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Security / Safety Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "NPCI Verified",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Zero Internet Required",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Transactions run securely over GSM cellular signals via Government NPCI NUUP servers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // PIN Dialog
    if (uiState.showPinDialog) {
        PinInputDialog(
            title = "Enter UPI PIN",
            subtitle = "Authorizing offline payment to ${uiState.recipient}",
            amountText = "₹${uiState.amount}",
            pinLength = 6,
            onPinConfirmed = { pin -> viewModel.onPinConfirmed(pin) },
            onDismiss = { viewModel.onPinDialogDismissed() }
        )
    }
}
