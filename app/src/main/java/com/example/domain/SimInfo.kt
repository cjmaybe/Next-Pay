package com.example.domain

data class SimInfo(
    val slotIndex: Int, // 1-based (1 or 2)
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val isDefault: Boolean = false,
    val isJio: Boolean = false,
    val number: String = ""
)
