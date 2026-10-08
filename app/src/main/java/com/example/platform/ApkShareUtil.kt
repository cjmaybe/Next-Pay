package com.example.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ApkShareUtil {

    fun shareAppApk(context: Context) {
        try {
            val appPackage = context.applicationInfo
            val srcFile = File(appPackage.sourceDir)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    srcFile
                ))
                putExtra(Intent.EXTRA_SUBJECT, "Next Pay - Offline Payment App")
                putExtra(Intent.EXTRA_TEXT, "Next Pay lets you send money and check your bank balance offline via *99# USSD without any internet!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Next Pay Offline"))
        } catch (e: Exception) {
            // Fallback plain share
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Next Pay - Offline Payment App")
                putExtra(Intent.EXTRA_TEXT, "Next Pay: Offline UPI money transfer & balance check via *99# USSD.")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(textIntent, "Share Next Pay"))
        }
    }
}
