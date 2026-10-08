package com.example.domain

object PinMasking {
    fun mask(pin: String): String {
        return "•".repeat(pin.length)
    }

    fun sanitizeForLogging(input: String): String {
        // Redact any sequence of 4-6 digits that looks like a PIN
        return input.replace(Regex("\\b\\d{4,6}\\b"), "[REDACTED]")
    }
}
