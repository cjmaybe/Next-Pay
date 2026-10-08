package com.example.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val transactions: Flow<List<TransactionEntity>> = allTransactions
    val totalSpent: Flow<Double?> = transactionDao.getTotalSpent()

    fun getTransactionsByStatus(status: TransactionStatus): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByStatus(status)

    fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
        transactionDao.searchTransactions(query)

    suspend fun getTransactionById(id: Long): TransactionEntity? =
        transactionDao.getTransactionById(id)

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun recordTransaction(
        vpa: String,
        payeeName: String?,
        amount: String,
        note: String?,
        carrierReply: String
    ): Long {
        val amt = amount.toDoubleOrNull() ?: 0.0
        val entity = TransactionEntity(
            recipient = vpa,
            recipientName = payeeName,
            amount = amt,
            status = TransactionStatus.SUCCESS,
            type = TransactionType.PAYMENT,
            notes = note,
            rawResponse = carrierReply,
            timestamp = System.currentTimeMillis()
        )
        return insertTransaction(entity)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    suspend fun clearHistory() =
        transactionDao.clearAll()
}
