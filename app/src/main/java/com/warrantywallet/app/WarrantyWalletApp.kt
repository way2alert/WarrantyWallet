package com.warrantywallet.app

import android.app.Application
import com.warrantywallet.app.core.database.WarrantyWalletDatabase
import com.warrantywallet.app.core.datastore.PreferenceManager
import com.warrantywallet.app.core.notification.AlarmScheduler
import com.warrantywallet.app.core.notification.NotificationHelper
import com.warrantywallet.app.data.remote.FirestoreSyncService
import com.warrantywallet.app.data.repository.AuthRepository
import com.warrantywallet.app.data.repository.ProductRepository

class WarrantyWalletApp : Application() {

    lateinit var database: WarrantyWalletDatabase
        private set

    lateinit var preferenceManager: PreferenceManager
        private set

    lateinit var alarmScheduler: AlarmScheduler
        private set

    lateinit var firestoreSyncService: FirestoreSyncService
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var productRepository: ProductRepository
        private set

    override fun onCreate() {
        super.onCreate()

        NotificationHelper.createNotificationChannels(this)

        database = WarrantyWalletDatabase.getInstance(this)
        preferenceManager = PreferenceManager(this)
        alarmScheduler = AlarmScheduler(this)
        firestoreSyncService = FirestoreSyncService()
        authRepository = AuthRepository(preferenceManager)
        productRepository = ProductRepository(
            productDao = database.productDao(),
            reminderDao = database.reminderDao(),
            documentDao = database.documentDao(),
            alarmScheduler = alarmScheduler,
            firestoreSync = firestoreSyncService,
            preferenceManager = preferenceManager
        )
    }
}
