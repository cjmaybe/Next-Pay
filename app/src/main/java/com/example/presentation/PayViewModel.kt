package com.example.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.NextPayApplication
import com.example.data.HistoryRepository
import com.example.data.PreferencesRepository
import com.example.domain.ActionEvent
import com.example.domain.ActionRun
import com.example.domain.ActionRunner
import com.example.domain.Actions
import com.example.domain.FormField
import com.example.domain.InputValidator
import com.example.domain.OperationMode
import com.example.domain.SessionState
import com.example.domain.SimInfo
import com.example.domain.UpiData
import com.example.domain.UpiParser
import com.example.platform.CarrierDetector
import com.example.platform.OverlayController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PayUiState(
    val vpa: String = "",
    val payeeName: String = "",
    val amount: String = "",
    val note: String = "",
    val pin: String = "",
    val selectedSimSlot: Int = 1,
    val availableSims: List<SimInfo> = emptyList(),
    val errors: Map<FormField, String> = emptyMap(),
    val isSessionActive: Boolean = false,
    val showPinDialog: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isManualMode: Boolean = false
) {
    val recipient: String get() = vpa
    val recipientName: String get() = payeeName
    val recipientError: String? get() = errors[FormField.VPA]
    val amountError: String? get() = errors[FormField.AMOUNT]
}

class PayViewModel(
    private val actionRunner: ActionRunner = NextPayApplication.instance.actionRunner,
    private val historyRepo: HistoryRepository = NextPayApplication.instance.historyRepository,
    private val prefsRepo: PreferencesRepository = NextPayApplication.instance.preferencesRepository,
    private val carrierDetector: CarrierDetector = NextPayApplication.instance.carrierDetector,
    private val overlayController: OverlayController? = NextPayApplication.instance.overlayController,
    private val onDialerFallback: (String) -> Unit = { code ->
        val context = NextPayApplication.instance
        val encoded = code.replace("#", Uri.encode("#"))
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encoded")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    },
    private val clipboardWriter: (String) -> Unit = { text ->
        val context = NextPayApplication.instance
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("UPI ID", text))
    },
    private val systemToast: (String) -> Unit = { msg ->
        Toast.makeText(NextPayApplication.instance, msg, Toast.LENGTH_LONG).show()
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayUiState())
    val uiState: StateFlow<PayUiState> = _uiState.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    val operationMode: StateFlow<OperationMode> = prefsRepo.operationModeEnum
        .stateIn(viewModelScope, SharingStarted.Eagerly, OperationMode.AUTO)

    private var activeRun: ActionRun? = null
    private var sessionJob: Job? = null

    init {
        loadSims()
    }

    fun loadSims() {
        viewModelScope.launch {
            val sims = carrierDetector.getAvailableSims()
            _uiState.update { it.copy(availableSims = sims) }
        }
    }

    fun prefillFromTransaction(vpa: String, amount: String, note: String?) {
        _uiState.update { current ->
            current.copy(
                vpa = vpa,
                amount = amount,
                note = note ?: "",
                pin = "",
                errors = emptyMap()
            )
        }
    }

    fun setFromQr(upiData: UpiData) {
        _uiState.update { current ->
            current.copy(
                vpa = upiData.vpa,
                payeeName = upiData.payeeName ?: current.payeeName,
                amount = upiData.amount ?: current.amount,
                note = upiData.transactionNote ?: current.note,
                errors = emptyMap()
            )
        }
    }

    fun onQrScanned(raw: String) {
        val upiData: UpiData = UpiParser.parse(raw) ?: return
        _uiState.update { current ->
            current.copy(
                vpa = upiData.vpa,
                payeeName = upiData.payeeName ?: current.payeeName,
                amount = upiData.amount ?: current.amount,
                note = upiData.transactionNote ?: current.note,
                errors = emptyMap()
            )
        }
    }

    fun onFormFieldChanged(
        vpa: String? = null,
        amount: String? = null,
        note: String? = null
    ) {
        _uiState.update { current ->
            val newErrors = current.errors.toMutableMap()
            if (vpa != null) newErrors.remove(FormField.VPA)
            if (amount != null) newErrors.remove(FormField.AMOUNT)
            current.copy(
                vpa = vpa ?: current.vpa,
                amount = amount ?: current.amount,
                note = note ?: current.note,
                errors = newErrors
            )
        }
    }

    fun onRecipientChanged(newRecipient: String) {
        onFormFieldChanged(vpa = newRecipient)
    }

    fun onAmountChanged(newAmount: String) {
        onFormFieldChanged(amount = newAmount)
    }

    fun onNoteChanged(newNote: String) {
        onFormFieldChanged(note = newNote)
    }

    fun setQuickAmount(amountVal: Int) {
        onFormFieldChanged(amount = amountVal.toString())
    }

    fun refreshSimsAndAccessibility() {
        viewModelScope.launch {
            val sims = carrierDetector.getAvailableSims()
            val mode = prefsRepo.operationModeEnum.first()
            val isA11y = com.example.platform.UssdAccessibilityService.isServiceRunning
            _uiState.update {
                it.copy(
                    availableSims = sims,
                    isAccessibilityEnabled = isA11y,
                    isManualMode = mode == OperationMode.MANUAL
                )
            }
        }
    }

    fun onPinDialogDismissed() {
        _uiState.update { it.copy(showPinDialog = false) }
    }

    fun resetSessionState() {
        dismissSession()
    }

    fun onProceedToPayClicked() {
        val state = _uiState.value
        val vpaValidation = InputValidator.validateVpa(state.vpa)
        val amtValidation = InputValidator.validateAmount(state.amount)

        val errors = mutableMapOf<FormField, String>()
        if (!vpaValidation.isValid) errors[FormField.VPA] = vpaValidation.errorMessage ?: "Invalid recipient"
        if (!amtValidation.isValid) errors[FormField.AMOUNT] = amtValidation.errorMessage ?: "Invalid amount"

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        // If in MANUAL mode, proceed immediately to dialer
        if (operationMode.value == OperationMode.MANUAL) {
            attemptPayment()
            return
        }

        // Prompt for UPI PIN
        _uiState.update { it.copy(errors = emptyMap(), showPinDialog = true) }
    }

    fun onPinConfirmed(pin: String) {
        onPinChanged(pin)
        attemptPayment()
    }

    fun onTestSimulationClicked() {
        val state = _uiState.value
        val recipient = state.vpa.ifBlank { "alice@upi" }
        val amount = state.amount.ifBlank { "500" }
        val note = state.note.ifBlank { "Next Pay v2 Test" }
        _uiState.update { it.copy(vpa = recipient, amount = amount, note = note, pin = "123456") }
        runSimulatedPayment(recipient, amount, note)
    }

    private fun runSimulatedPayment(vpa: String, amount: String, note: String) {
        sessionJob?.cancel()
        _uiState.update { it.copy(errors = emptyMap(), isSessionActive = true) }
        _sessionState.value = SessionState.Running(
            label = "Connecting to offline carrier network (*99#)",
            stepIndex = 0,
            total = Actions.SendUpi.steps.size
        )

        sessionJob = viewModelScope.launch {
            val steps = listOf(
                "Dialing *99*1*3# via SIM 1...",
                "Carrier connected: Sending UPI ID ($vpa)",
                "Carrier prompt: Sending amount (₹$amount)",
                "Adding remark ($note)",
                "Entering masked UPI PIN (******)",
                "Confirming transaction with bank..."
            )

            for (i in steps.indices) {
                delay(700)
                _sessionState.value = SessionState.Running(
                    label = steps[i],
                    stepIndex = i,
                    total = steps.size
                )
            }

            delay(800)
            val refId = (100000000000L..999999999999L).random().toString()
            val successMsg = "Payment of ₹$amount to $vpa successful. Ref No: $refId. Your bank account has been debited."
            _sessionState.value = SessionState.Success(resultText = successMsg)
            NextPayApplication.instance.triggerFeedback(isSuccess = true)
            onSessionEnded()

            // Simulation only: intentionally NOT written to transaction history,
            // otherwise fake payments would show up as real ones in History and totals.
        }
    }

    fun pasteFromClipboard(context: Context) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = cm?.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        val parsed = UpiParser.parse(clip)
        if (parsed != null) {
            onQrScanned(clip)
        } else {
            onRecipientChanged(clip.trim())
        }
    }

    fun onSimSlotSelected(slotIndex: Int) {
        _uiState.update { it.copy(selectedSimSlot = slotIndex) }
    }

    fun onPinChanged(pin: String) {
        val digits = pin.filter { it.isDigit() }.take(6)
        _uiState.update { current ->
            current.copy(
                pin = digits,
                errors = current.errors - FormField.PIN
            )
        }
    }

    fun attemptPayment() {
        val state = _uiState.value
        val mode = operationMode.value

        if (mode != OperationMode.MANUAL && !actionRunner.isServiceEnabled()) {
            _sessionState.value = SessionState.Failed(
                message = "Next Pay Accessibility Service is disabled. Please turn it on in Settings to automate offline USSD payments.",
                resultText = "Service Disabled"
            )
            return
        }

        val formValidation = InputValidator.validatePaymentForm(
            vpa = state.vpa,
            amount = state.amount,
            pin = state.pin
        )

        // In MANUAL mode, the user enters PIN inside the carrier dialog itself,
        // so we don't require the in-app PIN to be populated.
        val relevantErrors = if (mode == OperationMode.MANUAL) {
            formValidation.errors - FormField.PIN
        } else {
            formValidation.errors
        }

        if (relevantErrors.isNotEmpty()) {
            _uiState.update { it.copy(errors = relevantErrors) }
            return
        }

        val cleanedVpa = state.vpa.trim()
        val cleanedAmount = state.amount.trim()
        val cleanedNote = state.note.trim()

        if (mode == OperationMode.MANUAL) {
            clipboardWriter(cleanedVpa)
            _snackbar.value = "UPI ID copied — paste it on the *99# prompt"
            onDialerFallback("*99*1*3#")
            systemToast("UPI ID copied — please paste it from clipboard on *99# prompt")
            viewModelScope.launch {
                delay(1_800)
                systemToast("Paste UPI ID when the carrier asks for it")
            }
            _sessionState.value = SessionState.Idle
            return
        }

        runPayment(cleanedVpa, cleanedAmount, cleanedNote, state.pin)
    }

    private fun runPayment(vpa: String, amount: String, note: String, pin: String) {
        val mode = operationMode.value
        _uiState.update { it.copy(errors = emptyMap(), isSessionActive = true) }
        _sessionState.value = SessionState.Running(
            label = "Starting payment",
            stepIndex = 0,
            total = Actions.SendUpi.steps.size
        )

        when (mode) {
            OperationMode.AUTO -> {
                overlayController?.show(
                    title = "Paying ₹$amount",
                    subtitle = "to $vpa",
                    stepLabel = "STARTING"
                )
            }
            OperationMode.ADVANCED -> {
                overlayController?.showMinimal(
                    progress = 0,
                    total = Actions.SendUpi.steps.size,
                    label = "PAYING"
                )
            }
            OperationMode.MANUAL -> Unit
        }

        overlayController?.onCancel = { cancelSession() }

        val vars = mapOf(
            "vpa" to vpa,
            "amount" to amount,
            "note" to note.ifBlank { "Payment" },
            "pin" to pin
        )

        val run = actionRunner.runAction(Actions.SendUpi, vars, viewModelScope)
        activeRun = run

        sessionJob = viewModelScope.launch {
            launch {
                run.events.collect { event ->
                    val current = _sessionState.value
                    if (current is SessionState.Success || current is SessionState.Failed) {
                        return@collect
                    }

                    when (event) {
                        is ActionEvent.Progress -> {
                            _sessionState.value = SessionState.Running(
                                label = event.label ?: "Processing",
                                stepIndex = event.stepIndex,
                                total = event.total
                            )
                            when (mode) {
                                OperationMode.AUTO -> overlayController?.update(
                                    title = "Paying ₹$amount",
                                    subtitle = "to $vpa",
                                    stepLabel = (event.label ?: "Processing").uppercase()
                                )
                                OperationMode.ADVANCED -> overlayController?.updateMinimal(
                                    progress = event.stepIndex,
                                    total = event.total,
                                    label = "PAYING"
                                )
                                else -> Unit
                            }
                        }
                        is ActionEvent.Done -> {
                            _sessionState.value = SessionState.Success(resultText = event.resultText)
                            overlayController?.hide()
                            NextPayApplication.instance.triggerFeedback(isSuccess = true)
                            onSessionEnded()
                            launch {
                                historyRepo.recordTransaction(
                                    vpa = vpa,
                                    payeeName = _uiState.value.payeeName.ifBlank { null },
                                    amount = amount,
                                    note = note.ifBlank { null },
                                    carrierReply = event.resultText
                                )
                            }
                        }
                        is ActionEvent.Error -> {
                            _sessionState.value = SessionState.Failed(
                                message = event.message,
                                resultText = event.resultText
                            )
                            overlayController?.showError(
                                title = "Payment failed",
                                message = event.message
                            )
                            NextPayApplication.instance.triggerFeedback(isSuccess = false)
                            onSessionEnded()
                        }
                        else -> Unit
                    }
                }
            }

            val result = run.result.await()
            // A user cancel completes the run with "Cancelled"; cancelSession() already resets the UI,
            // so don't flip it into a "Payment failed" state.
            if (!result.success && result.resultText != "Cancelled" && _sessionState.value is SessionState.Running) {
                _sessionState.value = SessionState.Failed(
                    message = result.resultText,
                    resultText = result.resultText
                )
                overlayController?.showError(title = "Payment failed", message = result.resultText)
                NextPayApplication.instance.triggerFeedback(isSuccess = false)
                onSessionEnded()
            }
        }
    }

    fun cancelSession() {
        viewModelScope.launch {
            activeRun?.cancel?.invoke()
            overlayController?.hide()
            _sessionState.value = SessionState.Idle
            onSessionEnded()
        }
    }

    fun dismissSession() {
        val wasSuccess = _sessionState.value is SessionState.Success
        _sessionState.value = SessionState.Idle
        if (wasSuccess) {
            _uiState.update { PayUiState() }
        } else {
            _uiState.update { it.copy(pin = "") }
        }
    }

    fun clearFieldError(field: FormField) {
        _uiState.update { current ->
            if (current.errors.containsKey(field)) {
                current.copy(errors = current.errors - field)
            } else current
        }
    }

    fun dismissSnackbar() {
        _snackbar.value = null
    }

    fun onNavigateAway() {
        _uiState.update { it.copy(pin = "") }
    }

    fun onBackground() {
        _uiState.update { it.copy(pin = "") }
    }

    private fun onSessionEnded() {
        _uiState.update { it.copy(isSessionActive = false) }
        activeRun = null
        viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(pin = "") }
        }
    }
}
