package com.example.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.NextPayApplication
import com.example.data.HistoryRepository
import com.example.data.TransactionEntity
import com.example.data.TransactionStatus
import com.example.data.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter {
    ALL,
    PAYMENTS,
    BALANCE_CHECKS,
    SUCCESSFUL,
    FAILED
}

data class HistoryUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val totalSpent: Double = 0.0,
    val searchQuery: String = "",
    val activeFilter: HistoryFilter = HistoryFilter.ALL,
    val selectedTransaction: TransactionEntity? = null
)

class HistoryViewModel : ViewModel() {

    private val repository: HistoryRepository = NextPayApplication.instance.historyRepository

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(HistoryFilter.ALL)
    private val _selectedTransaction = MutableStateFlow<TransactionEntity?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.allTransactions,
        repository.totalSpent,
        _searchQuery,
        _activeFilter,
        _selectedTransaction
    ) { allTx, total, query, filter, selected ->
        val filtered = allTx.filter { tx ->
            val matchesQuery = if (query.isBlank()) true else {
                tx.recipient.contains(query, ignoreCase = true) ||
                        (tx.recipientName?.contains(query, ignoreCase = true) == true) ||
                        (tx.refId?.contains(query, ignoreCase = true) == true)
            }

            val matchesFilter = when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.PAYMENTS -> tx.type == TransactionType.PAYMENT
                HistoryFilter.BALANCE_CHECKS -> tx.type == TransactionType.BALANCE_CHECK
                HistoryFilter.SUCCESSFUL -> tx.status == TransactionStatus.SUCCESS
                HistoryFilter.FAILED -> tx.status == TransactionStatus.FAILED
            }

            matchesQuery && matchesFilter
        }

        HistoryUiState(
            transactions = filtered,
            totalSpent = total ?: 0.0,
            searchQuery = query,
            activeFilter = filter,
            selectedTransaction = selected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelected(filter: HistoryFilter) {
        _activeFilter.value = filter
    }

    fun onTransactionClicked(transaction: TransactionEntity) {
        _selectedTransaction.value = transaction
    }

    fun onDismissTransactionDetail() {
        _selectedTransaction.value = null
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            if (_selectedTransaction.value?.id == transaction.id) {
                _selectedTransaction.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
