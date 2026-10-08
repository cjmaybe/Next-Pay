package com.example.domain

/**
 * Pre-built USSD action definitions for supported operations.
 * Each action defines the USSD code, step sequence with regex matchers,
 * failure patterns, and timeout configuration.
 */
object Actions {

    /**
     * Common failure patterns shared across all USSD actions.
     */
    val COMMON_FAILURES: List<Regex> = listOf(
        Regex("\\bnot\\s+(a\\s+)?(valid|registered|recognised|recognized|allowed|found|active|enabled|known|supported)\\b", RegexOption.IGNORE_CASE),
        Regex("\\bnot\\s+(debited|sent|processed|completed)\\b", RegexOption.IGNORE_CASE),
        Regex("transaction\\s+(declined|failed|cancelled|denied|aborted)", RegexOption.IGNORE_CASE),
        Regex("unable\\s+to\\s+process", RegexOption.IGNORE_CASE),
        Regex("\\b(invalid|incorrect|wrong|bad)\\s+(pin|upi|vpa|amount|input|entry|id|password|number|account)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(pin|upi\\s*pin|upi\\s*id|vpa|password)\\s+(is\\s+)?(incorrect|invalid|wrong|mismatch)\\b", RegexOption.IGNORE_CASE),
        Regex("entered\\s+(upi\\s+)?pin\\s+(is\\s+)?(incorrect|invalid|wrong)", RegexOption.IGNORE_CASE),
        Regex("enter\\s+(a\\s+)?(valid|correct)", RegexOption.IGNORE_CASE),
        Regex("please\\s+enter\\s+(correct|valid)", RegexOption.IGNORE_CASE),
        Regex("please\\s+check\\s+and\\s+try\\s+again", RegexOption.IGNORE_CASE),
        Regex("psp\\s+(is\\s+)?not\\s+(registered|recognised|recognized)", RegexOption.IGNORE_CASE),
        Regex("vpa\\s+(does\\s+not\\s+exist|is\\s+not\\s+(registered|valid))", RegexOption.IGNORE_CASE),
        Regex("upi\\s*id\\s+(is\\s+)?(invalid|incorrect|wrong)", RegexOption.IGNORE_CASE),
        Regex("(no|not\\s+a)\\s+(account|user|customer)\\s+(found|registered|exists)", RegexOption.IGNORE_CASE),
        Regex("account\\s+(not\\s+found|does\\s+not\\s+exist)", RegexOption.IGNORE_CASE),
        Regex("user\\s+not\\s+found", RegexOption.IGNORE_CASE),
        Regex("(sender|payer).*(receiver|payee).*same|(receiver|payee).*(sender|payer).*same", RegexOption.IGNORE_CASE),
        Regex("\\bsame\\s+(account|vpa|user)\\b", RegexOption.IGNORE_CASE),
        Regex("cannot\\s+(send|pay)\\s+to\\s+(self|yourself|same)", RegexOption.IGNORE_CASE),
        Regex("insufficient\\s+(funds|balance)", RegexOption.IGNORE_CASE),
        Regex("exceed(s|ed)?\\s+(limit|amount)|over\\s+limit", RegexOption.IGNORE_CASE),
        Regex("service\\s+(unavailable|not\\s+available|down)", RegexOption.IGNORE_CASE),
        Regex("try\\s+again\\s+later|temporarily\\s+unavailable", RegexOption.IGNORE_CASE),
        Regex("session\\s+(timed\\s+out|expired|terminated)", RegexOption.IGNORE_CASE),
        Regex("could\\s+not\\s+find\\s+(your|ur)\\s+bank", RegexOption.IGNORE_CASE),
        Regex("is\\s+not\\s+a\\s+valid\\s+selection", RegexOption.IGNORE_CASE),
        Regex("please\\s+enter\\s+the\\s+correct\\s+no", RegexOption.IGNORE_CASE),
        Regex("bank\\s+not\\s+found|no\\s+bank\\s+(linked|found)", RegexOption.IGNORE_CASE)
    )

    /**
     * Send money via UPI using *99*1*3#.
     * 6-step flow: VPA → Amount → Remark → PIN → Confirm → Success.
     * 25-second hard timeout.
     */
    val SendUpi = Action(
        code = "*99*1*3#",
        steps = listOf(
            ActionStep(
                match = Regex("(receiver|payee|recipient|vpa|virtual.*payment|upi.*id)", RegexOption.IGNORE_CASE),
                reply = "{vpa}",
                label = "Sending UPI ID"
            ),
            ActionStep(
                match = Regex("\\bamount\\b", RegexOption.IGNORE_CASE),
                reply = "{amount}",
                label = "Sending amount"
            ),
            ActionStep(
                match = Regex("\\b(remark|comment|note)\\b", RegexOption.IGNORE_CASE),
                reply = "{note}",
                label = "Adding note"
            ),
            ActionStep(
                match = Regex("\\bupi\\s*pin\\b|\\b(enter|6\\s*digit).*pin\\b", RegexOption.IGNORE_CASE),
                reply = "{pin}",
                label = "Entering UPI PIN"
            ),
            ActionStep(
                match = Regex("\\b(confirm|press\\s*1|are you sure)\\b", RegexOption.IGNORE_CASE),
                reply = "1",
                label = "Confirming"
            ),
            ActionStep(
                match = Regex("successful|payment\\s+(?:sent|completed|done)|thank\\s*you\\s*for\\s*using|reference\\s+(?:no|number|id)\\s*[:\\-]", RegexOption.IGNORE_CASE),
                done = true,
                label = "Payment complete"
            )
        ),
        failurePatterns = COMMON_FAILURES,
        timeoutMs = 25_000L
    )

    /**
     * Check balance via *99*3#.
     * 2-step flow: PIN → Balance display.
     * 18-second hard timeout.
     */
    val CheckBalance = Action(
        code = "*99*3#",
        steps = listOf(
            ActionStep(
                match = Regex("upi\\s*pin|enter.*pin|6\\s*digit.*pin", RegexOption.IGNORE_CASE),
                reply = "{pin}",
                label = "Entering UPI PIN"
            ),
            ActionStep(
                match = Regex("balance|avail(able)?|rs\\.?\\s*\\d|inr\\s*\\d|₹\\s*\\d|amount\\s*(is|:)|ledger|a/c\\s*bal", RegexOption.IGNORE_CASE),
                done = true,
                label = "Balance fetched"
            )
        ),
        failurePatterns = COMMON_FAILURES + listOf(
            Regex("pin\\s*(does\\s*not|doesn['']?t)\\s*match", RegexOption.IGNORE_CASE),
            Regex("pin\\s*mismatch", RegexOption.IGNORE_CASE),
            Regex("authentication\\s*failed", RegexOption.IGNORE_CASE),
            Regex("max(imum)?\\s*(attempts|tries|retries)", RegexOption.IGNORE_CASE),
            Regex("pin\\s*(blocked|locked|expired)", RegexOption.IGNORE_CASE)
        ),
        timeoutMs = 18_000L
    )
}
