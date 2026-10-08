package com.example

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.AppDatabase
import com.example.data.HistoryRepository
import com.example.data.PreferencesRepository
import com.example.domain.ActionRunner
import com.example.domain.ModeRouter
import com.example.platform.CarrierDetector
import com.example.platform.OverlayControllerImpl
import com.example.platform.QrScannerManager
import com.example.platform.UssdEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NextPayApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var historyRepository: HistoryRepository
        private set
    lateinit var preferencesRepository: PreferencesRepository
        private set
    lateinit var carrierDetector: CarrierDetector
        private set
    lateinit var overlayController: OverlayControllerImpl
        private set
    lateinit var ussdEngine: UssdEngine
        private set
    lateinit var actionRunner: ActionRunner
        private set
    lateinit var modeRouter: ModeRouter
        private set
    lateinit var qrScannerManager: QrScannerManager
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var toneGenerator: ToneGenerator? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        historyRepository = HistoryRepository(database.transactionDao())
        preferencesRepository = PreferencesRepository(this)
        carrierDetector = CarrierDetector(this)
        overlayController = OverlayControllerImpl(this)
        ussdEngine = UssdEngine(this, overlayController)
        actionRunner = ActionRunner(ussdEngine)
        modeRouter = ModeRouter(this, ussdEngine, overlayController)
        qrScannerManager = QrScannerManager()

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    fun triggerFeedback(isSuccess: Boolean) {
        applicationScope.launch {
            val soundEnabled = preferencesRepository.soundFeedback.first()
            val vibrationEnabled = preferencesRepository.vibrationFeedback.first()

            if (soundEnabled) {
                try {
                    val tone = if (isSuccess) ToneGenerator.TONE_PROP_PROMPT else ToneGenerator.TONE_PROP_BEEP2
                    toneGenerator?.startTone(tone, 300)
                } catch (_: Exception) {
                }
            }

            if (vibrationEnabled) {
                try {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                        vibratorManager?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = if (isSuccess) {
                            VibrationEffect.createWaveform(longArrayOf(0, 100, 70, 150), -1)
                        } else {
                            VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                        }
                        vibrator?.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(250)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        toneGenerator?.release()
    }

    companion object {
        lateinit var instance: NextPayApplication
            private set
    }
}
