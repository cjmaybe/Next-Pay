package com.example.presentation

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.NextPayApplication
import com.example.data.PreferencesRepository
import com.example.domain.ActionEvent
import com.example.domain.ActionRun
import com.example.domain.ActionRunner
import com.example.domain.Actions
import com.example.domain.InputValidator
import com.example.domain.OperationMode
import com.example.domain.SessionState
import com.example.domain.SimInfo
import com.example.platform.CarrierDetector
import com.example.platform.OverlayController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BalanceResult(
    val text: String,
    val timestamp: Long
)

data class BalanceUiState(
    val pin: String = "",
    val pinError: String? = null,
    val isSessionActive: Boolean = false,
    val lastBalance: String = "",
    val lastBalanceAccount: String = "",
    val lastCheckedFormatted: String = "",
    val selectedSimSlot: Int = 1,
    val availableSims: List<SimInfo> = emptyList(),
    val showPinDialog: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isManualMode: Boolean = false
)

class BalanceViewModel(
    private val actionRunner: ActionRunner = NextPayApplication.instance.actionRunner,
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
    private val systemToast: (String) -> Unit = { msg ->
        Toast.makeText(NextPayApplication.instance, msg, Toast.LENGTH_LONG).show()
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow(BalanceUiState())
    val uiState: StateFlow<BalanceUiState> = _uiState.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    val lastResult: StateFlow<BalanceResult?> = combine(
        prefsRepo.lastBalanceText,
        prefsRepo.lastBalanceTimestamp
    ) { text, ts ->
        if (text != null && ts != null) BalanceResult(text, ts) else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val operationMode: StateFlow<OperationMode> = prefsRepo.operationModeEnum
        .stateIn(viewModelScope, SharingStarted.Eagerly, OperationMode.AUTO)

    private var activeRun: ActionRun? = null
    private var sessionJob: Job? = null

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val sims = carrierDetector.getAvailableSims()
            val defaultSlot = prefsRepo.defaultSimSlot.first()
            val lastBal = prefsRepo.lastBalance.first()
            val lastAcc = prefsRepo.lastBalanceAccount.first()
            val lastTs = prefsRepo.lastBalanceTimestamp.first() ?: 0L
            val mode = prefsRepo.operationModeEnum.first()
            val isA11y = com.example.platform.UssdAccessibilityService.isServiceRunning

            val formattedTime = if (lastTs > 0) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastTs))
            } else "Not checked yet"

            _uiState.update {
                it.copy(
                    availableSims = sims,
                    selectedSimSlot = if (sims.any { s -> s.slotIndex == defaultSlot }) defaultSlot else 1,
                    lastBalance = lastBal,
                    lastBalanceAccount = lastAcc,
                    lastCheckedFormatted = formattedTime,
                    isAccessibilityEnabled = isA11y,
                    isManualMode = mode == OperationMode.MANUAL
                )
            }
        }
    }

    fun resetSessionState() {
        dismissSession()
    }

    fun onTestSimulationClicked() {
        attemptCheckBalance("123456")
    }

    fun onSimSlotSelected(slot: Int) {
        _uiState.update { it.copy(selectedSimSlot = slot) }
    }

    fun onPinChanged(pin: String) {
        val digits = pin.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(pin = digits, pinError = null) }
    }

    fun onCheckBalanceClicked() {
        _uiState.update { it.copy(showPinDialog = true) }
    }

    fun onPinDialogDismissed() {
        _uiState.update { it.copy(showPinDialog = false) }
    }

    fun onPinConfirmed(pin: String) {
        _uiState.update { it.copy(showPinDialog = false, pin = pin) }
        attemptCheckBalance(pin)
    }

    fun attemptCheckBalance(pinOverride: String? = null) {
        val mode = operationMode.value
        val pin = pinOverride ?: _uiState.value.pin

        if (mode == OperationMode.MANUAL) {
            _snackbar.value = "Opening dialer for *99*3#"
            onDialerFallback("*99*3#")
            systemToast("Opening dialer for *99*3# (Check Balance)")
            _sessionState.value = SessionState.Idle
            return
        }

        if (!actionRunner.isServiceEnabled()) {
            _sessionState.value = SessionState.Failed(
                message = "Accessibility service is disabled. Please enable Next Pay in Accessibility settings.",
                resultText = ""
            )
            return
        }

        val validation = InputValidator.validatePin(pin)
        if (!validation.isValid) {
            _uiState.update { it.copy(pinError = validation.errorMessage) }
            return
        }

        runCheck(pin)
    }

    private fun runCheck(pin: String) {
        val mode = operationMode.value
        _uiState.update { it.copy(pinError = null, isSessionActive = true) }
        _sessionState.value = SessionState.Running(
            label = "Checking balance",
            stepIndex = 0,
            total = Actions.CheckBalance.steps.size
        )

        when (mode) {
            OperationMode.AUTO -> overlayController?.show(
                title = "Checking balance",
                subtitle = "Next Pay is asking your bank…",
                stepLabel = "STARTING"
            )
            OperationMode.ADVANCED -> overlayController?.showMinimal(
                progress = 0,
                total = Actions.CheckBalance.steps.size,
                label = "CHECKING BALANCE"
            )
            OperationMode.MANUAL -> Unit
        }

        overlayController?.onCancel = { cancelSession() }

        val vars = mapOf("pin" to pin)
        val run = actionRunner.runAction(Actions.CheckBalance, vars, viewModelScope)
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
                                    title = "Checking balance",
                                    subtitle = "Next Pay is asking your bank…",
                                    stepLabel = (event.label ?: "Processing").uppercase()
                                )
                                OperationMode.ADVANCED -> overlayController?.updateMinimal(
                                    progress = event.stepIndex,
                                    total = event.total,
                                    label = "CHECKING BALANCE"
                                )
                                else -> Unit
                            }
                        }
                        is ActionEvent.Done -> {
                            _sessionState.value = SessionState.Success(resultText = event.resultText)
                            overlayController?.hide()
                            NextPayApplication.instance.triggerFeedback(isSuccess = true)
                            launch {
                                prefsRepo.setLastBalance(
                                    text = event.resultText,
                                    timestamp = System.currentTimeMillis()
                                )
                                loadData()
                            }
                            onSessionEnd()
                        }
                        is ActionEvent.Error -> {
                            _sessionState.value = SessionState.Failed(
                                message = event.message,
                                resultText = event.resultText
                            )
                            overlayController?.showError(
                                title = "Check failed",
                                message = event.message
                            )
                            NextPayApplication.instance.triggerFeedback(isSuccess = false)
                            onSessionEnd()
                        }
                        else -> Unit
                    }
                }
            }

            val result = run.result.await()
            if (!result.success && _sessionState.value is SessionState.Running) {
                _sessionState.value = SessionState.Failed(
                    message = "Balance check failed",
                    resultText = result.resultText
                )
                NextPayApplication.instance.triggerFeedback(isSuccess = false)
                onSessionEnd()
            }
        }
    }

    fun cancelSession() {
        viewModelScope.launch {
            activeRun?.cancel?.invoke()
            overlayController?.hide()
            _sessionState.value = SessionState.Idle
            onSessionEnd()
        }
    }

    fun dismissSession() {
        _sessionState.value = SessionState.Idle
        _uiState.update { it.copy(pin = "") }
        loadData()
    }

    fun dismissSnackbar() {
        _snackbar.value = null
    }

    private fun onSessionEnd() {
        _uiState.update { it.copy(isSessionActive = false) }
        activeRun = null
        viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(pin = "") }
        }
    }
}
