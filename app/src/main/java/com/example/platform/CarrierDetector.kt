package com.example.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.domain.SimInfo

class CarrierDetector(private val context: Context) {

    fun getAvailableSims(): List<SimInfo> {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return listOf(
                SimInfo(
                    slotIndex = 1,
                    subscriptionId = 1,
                    displayName = "SIM 1 (Offline UPI)",
                    carrierName = "Default SIM",
                    isDefault = true
                )
            )
        }

        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val subList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                subscriptionManager?.activeSubscriptionInfoList ?: emptyList()
            } else {
                emptyList()
            }

            if (subList.isEmpty()) {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                val operator = telephonyManager?.networkOperatorName.takeIf { !it.isNullOrBlank() } ?: "Mobile Carrier"
                listOf(
                    SimInfo(
                        slotIndex = 1,
                        subscriptionId = 1,
                        displayName = "SIM 1",
                        carrierName = operator,
                        isDefault = true
                    )
                )
            } else {
                subList.mapIndexed { index, subInfo ->
                    val carrier = subInfo.carrierName?.toString() ?: "SIM ${subInfo.simSlotIndex + 1}"
                    val isJio = carrier.contains("jio", ignoreCase = true)
                    SimInfo(
                        slotIndex = subInfo.simSlotIndex + 1,
                        subscriptionId = subInfo.subscriptionId,
                        displayName = subInfo.displayName?.toString() ?: "SIM ${index + 1}",
                        carrierName = carrier,
                        isDefault = index == 0,
                        isJio = isJio,
                        number = subInfo.number ?: ""
                    )
                }
            }
        } catch (e: SecurityException) {
            listOf(
                SimInfo(
                    slotIndex = 1,
                    subscriptionId = 1,
                    displayName = "SIM 1",
                    carrierName = "Carrier",
                    isDefault = true
                )
            )
        }
    }
}
