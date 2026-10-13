package com.example.domain

import android.net.Uri

object UpiParser {

    /**
     * Parses a UPI QR string (e.g., upi://pay?pa=name@bank&pn=John&am=100.00&cu=INR)
     * or raw UPI ID (john@okhdfcbank) or 10-digit Indian phone number.
     */
    fun parse(input: String): UpiData? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // Check if it's a URI format
        if (trimmed.startsWith("upi://pay", ignoreCase = true)) {
            return try {
                val uri = Uri.parse(trimmed)
                val pa = uri.getQueryParameter("pa") ?: return null
                val pn = uri.getQueryParameter("pn")
                val am = uri.getQueryParameter("am")
                val tn = uri.getQueryParameter("tn")
                val mc = uri.getQueryParameter("mc")

                UpiData(
                    vpa = pa.trim(),
                    payeeName = pn?.trim(),
                    amount = am?.trim(),
                    transactionNote = tn?.trim(),
                    merchantCode = mc?.trim(),
                    rawQrString = trimmed
                )
            } catch (e: Exception) {
                null
            }
        }

        // Direct UPI ID format: username@bank
        val upiRegex = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z0-9]{2,64}$")
        if (upiRegex.matches(trimmed)) {
            return UpiData(vpa = trimmed, rawQrString = trimmed)
        }

        // 10-digit mobile number format
        val phoneRegex = Regex("^[6-9]\\d{9}$")
        val cleanPhone = trimmed.replace("+91", "").replace(" ", "").replace("-", "")
        if (phoneRegex.matches(cleanPhone)) {
            return UpiData(vpa = cleanPhone, rawQrString = trimmed)
        }

        // Check if text contains a URI substring
        val upiIndex = trimmed.indexOf("upi://pay", ignoreCase = true)
        if (upiIndex != -1) {
            val substring = trimmed.substring(upiIndex)
            val endIdx = substring.indexOfAny(charArrayOf(' ', '\n', '\r'))
            val cleanUri = if (endIdx != -1) substring.substring(0, endIdx) else substring
            return parse(cleanUri)
        }

        return null
    }

    /**
     * Identifies if the recipient is a phone number or VPA.
     */
    fun isPhoneNumber(recipient: String): Boolean {
        val clean = recipient.replace("+91", "").replace(" ", "").replace("-", "")
        return clean.length == 10 && clean.all { it.isDigit() }
    }
}
