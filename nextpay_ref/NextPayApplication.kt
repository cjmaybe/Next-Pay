package com.example

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.AppDatabase
import com.example.data.HistoryRepository
import com.example.data.PreferencesRepository
import com.example.domain.ActionRunner
import com.example.platform.CarrierDetector
import com.example.platform.OverlayControllerImpl
import com.example.platform.QrScannerManager
import com.example.platform.UssdEngine

/**
 * Top-level DataStore delegate (must be at file level per DataStore docs).
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Application class wiring all singleton dependencies via manual DI.
 *
 * Validates: Requirements 14.1, 14.2, 15.1
 */
class NextPayApplication : Application() {

    // ─── Data Layer ────────────────────────────────────────────────────────────

    lateinit var database: AppDatabase
        private set

    lateinit var historyRepo: HistoryRepository
        private set

    lateinit var prefsRepo: PreferencesRepository
        private set

    // ─── Platform Layer ────────────────────────────────────────────────────────

    lateinit var overlayController: OverlayControllerImpl
        private set

    lateinit var ussdEngine: UssdEngine
        private set

    lateinit var carrierDetector: CarrierDetector
        private set

    lateinit var qrScannerManager: QrScannerManager
        private set

    // ─── Domain Layer ──────────────────────────────────────────────────────────

    lateinit var actionRunner: ActionRunner
        private set

    override fun onCreate() {
        super.onCreate()

        // Data layer
        val passphrase = "nextpay_secure_db".toByteArray() // In production, derive from secure source
        database = AppDatabase.create(this, passphrase)
        historyRepo = HistoryRepository(database.transactionDao())
        prefsRepo = PreferencesRepository(dataStore)

        // Platform layer
        overlayController = OverlayControllerImpl(this)
        ussdEngine = UssdEngine(this, overlayController)
        carrierDetector = CarrierDetector(this)
        qrScannerManager = QrScannerManager()

        // Domain layer
        actionRunner = ActionRunner(ussdEngine)
    }
}

/**
 * Extension property for convenient access to [NextPayApplication] from any Context.
 */
val Context.nextPayApp: NextPayApplication
    get() = applicationContext as NextPayApplication
