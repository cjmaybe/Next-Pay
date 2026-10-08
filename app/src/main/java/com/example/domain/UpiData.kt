package com.example.domain

data class UpiData(
    val vpa: String,                   // Virtual Payment Address (UPI ID) or Phone Number
    val payeeName: String? = null,
    val amount: String? = null,
    val transactionNote: String? = null,
    val merchantCode: String? = null,
    val rawQrString: String? = null
)
