package com.example.domain

/**
 * Validates payment form inputs before session initiation.
 * All validation is performed locally with no network dependency.
 */
object InputValidator {

    private val VPA_PATTERN = Regex("^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$")
    private val PHONE_PATTERN = Regex("^[6-9]\\d{9}$")
    private const val VPA_MAX_LENGTH = 50
    private const val AMOUNT_MIN = 1.0
    private const val AMOUNT_MAX = 5000.0
    private val PIN_PATTERN = Regex("^\\d{4,6}$")
    private val DECIMAL_PLACES_PATTERN = Regex("^\\d+(\\.\\d{1,2})?$")

    /**
     * Validates a UPI VPA (Virtual Payment Address) or 10-digit Indian mobile number.
     */
    fun validateVpa(vpa: String): ValidationResult {
        val trimmed = vpa.trim().replace("+91", "").replace(" ", "").replace("-", "")
        if (trimmed.isEmpty()) {
            return ValidationResult(isValid = false, errorMessage = "VPA or phone number is required")
        }
        if (PHONE_PATTERN.matches(trimmed)) {
            return ValidationResult(isValid = true, errorMessage = null)
        }
        if (trimmed.length > VPA_MAX_LENGTH) {
            return ValidationResult(isValid = false, errorMessage = "VPA must be at most $VPA_MAX_LENGTH characters")
        }
        if (!VPA_PATTERN.matches(vpa.trim())) {
            return ValidationResult(isValid = false, errorMessage = "Invalid VPA format (e.g. name@bank or 10-digit mobile)")
        }
        return ValidationResult(isValid = true, errorMessage = null)
    }

    /**
     * Validates a payment amount string.
     * Must parse as a number between ₹1 and ₹5000 with at most 2 decimal places.
     */
    fun validateAmount(amount: String): ValidationResult {
        val trimmed = amount.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult(isValid = false, errorMessage = "Amount is required")
        }
        if (!DECIMAL_PLACES_PATTERN.matches(trimmed)) {
            return ValidationResult(isValid = false, errorMessage = "Amount must have at most 2 decimal places")
        }
        val value = trimmed.toDoubleOrNull()
            ?: return ValidationResult(isValid = false, errorMessage = "Invalid amount")
        if (value < AMOUNT_MIN) {
            return ValidationResult(isValid = false, errorMessage = "Amount must be at least ₹1")
        }
        if (value > AMOUNT_MAX) {
            return ValidationResult(isValid = false, errorMessage = "Amount must not exceed ₹5000")
        }
        return ValidationResult(isValid = true, errorMessage = null)
    }

    /**
     * Validates a UPI PIN string.
     * Must be exactly 4 to 6 digits (0-9 only).
     */
    fun validatePin(pin: String): ValidationResult {
        if (pin.isEmpty()) {
            return ValidationResult(isValid = false, errorMessage = "PIN is required")
        }
        if (!PIN_PATTERN.matches(pin)) {
            return ValidationResult(isValid = false, errorMessage = "PIN must be 4-6 digits")
        }
        return ValidationResult(isValid = true, errorMessage = null)
    }

    /**
     * Validates the entire payment form.
     */
    fun validatePaymentForm(vpa: String, amount: String, pin: String): FormValidationResult {
        val errors = mutableMapOf<FormField, String>()

        val vpaResult = validateVpa(vpa)
        if (!vpaResult.isValid) {
            errors[FormField.VPA] = vpaResult.errorMessage!!
        }

        val amountResult = validateAmount(amount)
        if (!amountResult.isValid) {
            errors[FormField.AMOUNT] = amountResult.errorMessage!!
        }

        val pinResult = validatePin(pin)
        if (!pinResult.isValid) {
            errors[FormField.PIN] = pinResult.errorMessage!!
        }

        return FormValidationResult(errors = errors)
    }
}
