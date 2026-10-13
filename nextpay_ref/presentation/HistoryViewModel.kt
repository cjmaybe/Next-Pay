package com.example.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryRepository
import com.example.data.TransactionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val historyRepo: HistoryRepository
) : ViewModel() {

    val transactions: StateFlow<List<TransactionEntity>> = historyRepo.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Delete a single transaction by id. */
    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            historyRepo.delete(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepo.clearHistory()
        }
    }
}
