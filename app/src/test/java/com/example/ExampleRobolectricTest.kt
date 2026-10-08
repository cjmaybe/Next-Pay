package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.ActionRunner
import com.example.domain.Actions
import com.example.domain.FrameFilter
import com.example.domain.InputValidator
import com.example.domain.UpiParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from string resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Next Pay", appName)
    }

    @Test
    fun `upi parser parses standard upi uri correctly`() {
        val uri = "upi://pay?pa=merchant@okhdfcbank&pn=SuperStore&am=250.00&cu=INR&tn=Groceries"
        val data = UpiParser.parse(uri)
        assertNotNull(data)
        assertEquals("merchant@okhdfcbank", data?.vpa)
        assertEquals("SuperStore", data?.payeeName)
        assertEquals("250.00", data?.amount)
        assertEquals("Groceries", data?.transactionNote)
    }

    @Test
    fun `input validator enforces nuup transaction limits`() {
        assertTrue(InputValidator.validateAmount("100").isValid)
        assertTrue(InputValidator.validateAmount("5000").isValid)
        assertFalse(InputValidator.validateAmount("5001").isValid)
        assertFalse(InputValidator.validateAmount("0").isValid)

        assertTrue(InputValidator.validatePin("1234").isValid)
        assertTrue(InputValidator.validatePin("123456").isValid)
        assertFalse(InputValidator.validatePin("12").isValid)

        assertTrue(InputValidator.validateVpa("9876543210").isValid)
        assertTrue(InputValidator.validateVpa("shop@upi").isValid)
    }

    @Test
    fun `frame filter suppresses system placeholders and duplicates`() {
        val filter = FrameFilter()
        assertTrue(filter.isSystemPlaceholder("USSD code running..."))
        assertTrue(filter.isSystemPlaceholder("Please wait"))
        assertTrue(filter.isSystemPlaceholder("Processing"))

        assertFalse(filter.shouldEmit("Please wait"))
        assertTrue(filter.shouldEmit("Enter UPI PIN"))
        assertFalse(filter.shouldEmit("Enter UPI PIN")) // Duplicate
        assertTrue(filter.shouldEmit("Payment Successful"))
    }

    @Test
    fun `action runner step matcher matches ussd frames`() {
        val steps = Actions.SendUpi.steps
        val vpaMatch = steps[0].match.containsMatchIn("Enter receiver UPI ID / VPA:")
        assertTrue("Step 0 should match VPA prompt", vpaMatch)

        val amountMatch = steps[1].match.containsMatchIn("Enter Amount (Rs):")
        assertTrue("Step 1 should match Amount prompt", amountMatch)

        val pinMatch = steps[3].match.containsMatchIn("Enter 6 digit UPI PIN:")
        assertTrue("Step 3 should match PIN prompt", pinMatch)

        assertTrue(ActionRunner.UNIVERSAL_SUCCESS_PATTERNS.any {
            it.containsMatchIn("Your transaction of Rs 100 is successful. Ref Id: 123456789012")
        })
    }
}
