package com.example.domain

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Holds the running state of an action execution.
 */
data class ActionRun(
    val events: SharedFlow<ActionEvent>,
    val cancel: suspend () -> Unit,
    val result: Deferred<ActionResult>
)

/**
 * Executes scripted USSD action flows against the [UssdEnginePort].
 * Pure domain logic with no Android framework dependencies.
 */
class ActionRunner(private val engine: UssdEnginePort) {

    companion object {
        const val DEFAULT_PACING_DELAY_MS = 250L

        val UNIVERSAL_SUCCESS_PATTERNS: List<Regex> = listOf(
            Regex("\\bis\\s+successful\\b", RegexOption.IGNORE_CASE),
            Regex("payment\\s+(?:to\\s+\\S+\\s+)?(?:for\\s+)?(?:rs\\.?|inr|₹)?\\s*\\d.*successful", RegexOption.IGNORE_CASE),
            Regex("successfully\\s+(?:sent|paid|completed|debited|transferred)", RegexOption.IGNORE_CASE),
            Regex("transaction\\s+successful", RegexOption.IGNORE_CASE),
            Regex("txn\\s+successful", RegexOption.IGNORE_CASE),
            Regex("payment\\s+successful", RegexOption.IGNORE_CASE),
            Regex("ref(?:erence)?\\s*(?:id|no|number|#)?\\s*[:\\-]?\\s*\\d{6,}", RegexOption.IGNORE_CASE),
            Regex("debited\\s+(by\\s+)?(rs\\.?|inr|₹)?\\s*\\d+", RegexOption.IGNORE_CASE),
            Regex("upi\\s+ref(?:erence)?\\s*(?:id|no|number|#)?\\s*[:\\-]?\\s*\\d+", RegexOption.IGNORE_CASE),
            Regex("money\\s+(transferred|sent)", RegexOption.IGNORE_CASE)
        )
    }

    /**
     * Checks whether the AccessibilityService is currently running.
     */
    fun isServiceEnabled(): Boolean = engine.isServiceEnabled()

    /**
     * Starts executing an action flow.
     */
    fun runAction(
        action: Action,
        vars: Map<String, String>,
        scope: CoroutineScope
    ): ActionRun {
        val eventFlow = MutableSharedFlow<ActionEvent>(extraBufferCapacity = 32)
        val resultDeferred = CompletableDeferred<ActionResult>()
        var job: Job? = null

        job = scope.launch {
            var currentStepIndex = 0
            val totalSteps = action.steps.size
            var terminated = false

            try {
                // 1. Dismiss any leftover dialog and dial the action code.
                engine.dismissDialog()
                engine.dial(action.code)

                // 2. Capture our session ID.
                val mySessionId = engine.getSessionId()

                // 3. Walk frames as they arrive.
                engine.frames.collect { frame ->
                    if (terminated) return@collect
                    if (frame.sessionId != mySessionId) return@collect

                    val text = frame.text

                    // ── Priority 1: universal success short-circuit ────────
                    if (matchesUniversalSuccess(text)) {
                        terminated = true
                        eventFlow.emit(ActionEvent.Done(text))
                        resultDeferred.complete(ActionResult(success = true, resultText = text))
                        engine.dismissDialog()
                        engine.cancel()
                        job?.cancel()
                        return@collect
                    }

                    // ── Priority 2: action-specific failure patterns ───────
                    if (matchesFailurePattern(text, action.failurePatterns)) {
                        terminated = true
                        eventFlow.emit(ActionEvent.Error("Transaction failed", text))
                        resultDeferred.complete(ActionResult(success = false, resultText = text))
                        engine.dismissDialog()
                        engine.cancel()
                        job?.cancel()
                        return@collect
                    }

                    // ── Priority 3: step matching ──────────────────────────
                    val matchedIndex = matchStep(text, action.steps, currentStepIndex)

                    if (matchedIndex >= 0) {
                        val step = action.steps[matchedIndex]
                        currentStepIndex = matchedIndex + 1

                        eventFlow.emit(ActionEvent.Progress(matchedIndex, totalSteps, step.label))
                        eventFlow.emit(ActionEvent.Frame(frame, matchedIndex))

                        if (step.done) {
                            terminated = true
                            eventFlow.emit(ActionEvent.Done(text))
                            resultDeferred.complete(ActionResult(success = true, resultText = text))
                            engine.dismissDialog()
                            engine.cancel()
                            job?.cancel()
                            return@collect
                        }

                        val reply = step.reply?.let { fillTemplate(it, vars) }
                        if (reply != null) {
                            if (!engine.isServiceEnabled()) {
                                val errorMsg = "Accessibility service was killed — re-enable it in settings"
                                terminated = true
                                eventFlow.emit(ActionEvent.Error(errorMsg, text))
                                resultDeferred.complete(ActionResult(success = false, resultText = errorMsg))
                                engine.cancel()
                                job?.cancel()
                                return@collect
                            }

                            delay(step.delayMs)

                            engine.sendReply(reply)
                            eventFlow.emit(ActionEvent.Reply(reply, matchedIndex))
                        }
                        return@collect
                    }

                    // ── Priority 4: terminal frame with no match ───────────
                    if (frame.isTerminal) {
                        terminated = true
                        eventFlow.emit(ActionEvent.Error("Unexpected carrier response", text))
                        resultDeferred.complete(ActionResult(success = false, resultText = text))
                        engine.dismissDialog()
                        engine.cancel()
                        job?.cancel()
                        return@collect
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (!resultDeferred.isCompleted) {
                    resultDeferred.complete(ActionResult(success = false, resultText = "Cancelled"))
                }
            } catch (e: Exception) {
                if (!resultDeferred.isCompleted) {
                    val errorMsg = e.message ?: "Unknown error"
                    eventFlow.emit(ActionEvent.Error(errorMsg, ""))
                    resultDeferred.complete(ActionResult(success = false, resultText = errorMsg))
                }
            }
        }

        val cancelFn: suspend () -> Unit = {
            engine.cancel()
            if (!resultDeferred.isCompleted) {
                resultDeferred.complete(ActionResult(success = false, resultText = "Cancelled"))
            }
            job.cancel()
        }

        return ActionRun(
            events = eventFlow.asSharedFlow(),
            cancel = cancelFn,
            result = resultDeferred
        )
    }

    fun matchStep(frameText: String, steps: List<ActionStep>, fromIndex: Int): Int {
        for (i in fromIndex until steps.size) {
            if (steps[i].match.containsMatchIn(frameText)) {
                return i
            }
        }
        return -1
    }

    fun fillTemplate(template: String, vars: Map<String, String>): String {
        var result = template
        for ((key, value) in vars) {
            result = result.replace("{$key}", value)
        }
        return result
    }

    fun matchesUniversalSuccess(text: String): Boolean =
        UNIVERSAL_SUCCESS_PATTERNS.any { it.containsMatchIn(text) }

    fun matchesFailurePattern(text: String, patterns: List<Regex>): Boolean =
        patterns.any { it.containsMatchIn(text) }
}
