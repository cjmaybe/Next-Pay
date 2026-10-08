package com.example.presentation.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.data.TransactionStatus
import com.example.data.TransactionType
import com.example.presentation.HistoryFilter
import com.example.presentation.HistoryViewModel
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("history_search_input"),
            placeholder = { Text("Search by recipient, name, or Ref ID...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryFilter.entries.forEach { filter ->
                FilterChip(
                    selected = uiState.activeFilter == filter,
                    onClick = { viewModel.onFilterSelected(filter) },
                    label = {
                        Text(
                            text = when (filter) {
                                HistoryFilter.ALL -> "All"
                                HistoryFilter.PAYMENTS -> "Payments"
                                HistoryFilter.BALANCE_CHECKS -> "Balance Checks"
                                HistoryFilter.SUCCESSFUL -> "Success"
                                HistoryFilter.FAILED -> "Failed"
                            }
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Sent Offline",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", uiState.totalSpent)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (uiState.transactions.isNotEmpty()) {
                    TextButton(onClick = { showClearConfirmDialog = true }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Transaction List
        if (uiState.transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No transactions found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Payments made via Next Pay will appear here offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.transactions, key = { it.id }) { tx ->
                    TransactionItem(
                        transaction = tx,
                        onClick = { viewModel.onTransactionClicked(tx) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Detail Bottom Sheet
    uiState.selectedTransaction?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            onDismiss = { viewModel.onDismissTransactionDetail() },
            onDelete = { viewModel.deleteTransaction(tx) },
            onShare = {
                val text = "Next Pay Receipt\nTo: ${tx.recipient}\nAmount: ₹${tx.amount}\nStatus: ${tx.status}\nRef: ${tx.refId ?: "N/A"}"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Next Pay Receipt")
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
            }
        )
    }

    // Clear Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Transaction History?") },
            text = { Text("This will permanently remove all offline transaction logs from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearAllHistory()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(transaction.timestamp) { dateFormat.format(Date(transaction.timestamp)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            transaction.type == TransactionType.BALANCE_CHECK -> MaterialTheme.colorScheme.secondaryContainer
                            transaction.status == TransactionStatus.SUCCESS -> SuccessGreen.copy(alpha = 0.15f)
                            else -> ErrorRed.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        transaction.type == TransactionType.BALANCE_CHECK -> Icons.Default.AccountBalanceWallet
                        transaction.status == TransactionStatus.SUCCESS -> Icons.Default.ArrowUpward
                        else -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when {
                        transaction.type == TransactionType.BALANCE_CHECK -> MaterialTheme.colorScheme.onSecondaryContainer
                        transaction.status == TransactionStatus.SUCCESS -> SuccessGreen
                        else -> ErrorRed
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Payee & Timestamp
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.recipientName ?: transaction.recipient,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount & Status
            Column(horizontalAlignment = Alignment.End) {
                if (transaction.type == TransactionType.BALANCE_CHECK) {
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Text(
                        text = "₹${transaction.amount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = transaction.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (transaction.status) {
                        TransactionStatus.SUCCESS -> SuccessGreen
                        TransactionStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                        TransactionStatus.FAILED -> ErrorRed
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(transaction.timestamp) { dateFormat.format(Date(transaction.timestamp)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (transaction.status == TransactionStatus.SUCCESS) SuccessGreen.copy(alpha = 0.15f)
                        else ErrorRed.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.status == TransactionStatus.SUCCESS) Icons.Default.CheckCircle
                    else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (transaction.status == TransactionStatus.SUCCESS) SuccessGreen else ErrorRed,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (transaction.type == TransactionType.BALANCE_CHECK) "Bank Balance Checked" else "₹${transaction.amount}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = transaction.status.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (transaction.status == TransactionStatus.SUCCESS) SuccessGreen else ErrorRed
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(label = "Recipient / Action", value = transaction.recipient)
            if (transaction.recipientName != null) {
                DetailRow(label = "Payee Name", value = transaction.recipientName)
            }
            DetailRow(label = "Date & Time", value = formattedDate)
            DetailRow(label = "Ref / Transaction ID", value = transaction.refId ?: "N/A")
            DetailRow(label = "Method", value = "Offline *99# USSD")
            if (transaction.notes != null) {
                DetailRow(label = "Details", value = transaction.notes)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
