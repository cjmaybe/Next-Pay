package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import com.example.platform.OverlayController
import com.example.platform.UssdEngine

/**
 * Routes USSD session initiation based on the selected [OperationMode].
 *
 * - [OperationMode.MANUAL]: Opens the system dialer (ACTION_DIAL) with the
 *   USSD code prefilled. No automation. The clipboard copy and snackbar
 *   are handled at the ViewModel layer.
 * - [OperationMode.ADVANCED]: Automates the session via [UssdEngine.dial]
 *   and shows a small floating progress chip via [OverlayController.showMinimal];
 *   the carrier dialog stays visible.
 * - [OperationMode.AUTO]: Same automation, plus the full-screen branded
 *   overlay via [OverlayController.show] that hides the carrier dialog.
 */
class ModeRouter(
    private val context: Context,
    private val ussdEngine: UssdEngine,
    private val overlayController: OverlayController? = null
) {
    suspend fun startSession(
        mode: OperationMode,
        code: String,
        title: String = "Processing",
        subtitle: String = "Starting session..."
    ): Boolean {
        return when (mode) {
            OperationMode.MANUAL -> {
                val encoded = code.replace("#", Uri.encode("#"))
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encoded"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                false
            }
            OperationMode.ADVANCED -> {
                overlayController?.showMinimal(progress = 0, total = 1, label = title)
                ussdEngine.dial(code)
                true
            }
            OperationMode.AUTO -> {
                overlayController?.show(title, subtitle, "")
                ussdEngine.dial(code)
                true
            }
        }
    }

    companion object {
        fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
            val expectedComponentName = "${context.packageName}/${serviceClass.canonicalName}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
