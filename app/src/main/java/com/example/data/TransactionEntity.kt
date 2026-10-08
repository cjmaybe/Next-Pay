package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionStatus {
    SUCCESS,
    PENDING,
    FAILED
}

enum class TransactionType {
    PAYMENT,
    BALANCE_CHECK
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val recipient: String, // UPI ID or Phone Number
    val recipientName: String? = null,
    val amount: Double = 0.0,
    val status: TransactionStatus = TransactionStatus.SUCCESS,
    val refId: String? = null,
    val simSlot: Int = 1,
    val carrierName: String? = null,
    val type: TransactionType = TransactionType.PAYMENT,
    val notes: String? = null,
    val rawResponse: String? = null
)
