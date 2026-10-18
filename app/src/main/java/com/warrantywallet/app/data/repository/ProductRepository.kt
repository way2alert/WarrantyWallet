package com.warrantywallet.app.data.repository

import com.warrantywallet.app.core.database.dao.DocumentDao
import com.warrantywallet.app.core.database.dao.ProductDao
import com.warrantywallet.app.core.database.dao.ReminderDao
import com.warrantywallet.app.core.database.entity.DocumentEntity
import com.warrantywallet.app.core.database.entity.ProductEntity
import com.warrantywallet.app.core.database.entity.ReminderEntity
import com.warrantywallet.app.core.datastore.PreferenceManager
import com.warrantywallet.app.core.model.DocumentItem
import com.warrantywallet.app.core.model.Product
import com.warrantywallet.app.core.model.Reminder
import com.warrantywallet.app.core.model.ReminderType
import com.warrantywallet.app.core.model.SyncStatus
import com.warrantywallet.app.core.notification.AlarmScheduler
import com.warrantywallet.app.core.util.DateUtils
import com.warrantywallet.app.data.remote.FirestoreSyncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProductRepository(
    private val productDao: ProductDao,
    private val reminderDao: ReminderDao,
    private val documentDao: DocumentDao,
    private val alarmScheduler: AlarmScheduler,
    private val firestoreSync: FirestoreSyncService,
    private val preferenceManager: PreferenceManager
) {

    fun getAllProductsFlow(): Flow<List<Product>> {
        return productDao.getAllProductsFlow().map { list -> list.map { it.toDomain() } }
    }

    fun getProductByIdFlow(id: String): Flow<Product?> {
        return productDao.getProductByIdFlow(id).map { it?.toDomain() }
    }

    suspend fun getProductById(id: String): Product? {
        return productDao.getProductById(id)?.toDomain()
    }

    fun getExpiringSoonProductsFlow(thresholdDays: Int = 30): Flow<List<Product>> {
        val now = System.currentTimeMillis()
        val thresholdMillis = now + (thresholdDays.toLong() * 24L * 60L * 60L * 1000L)
        return productDao.getExpiringSoonProductsFlow(now, thresholdMillis).map { list -> list.map { it.toDomain() } }
    }

    fun searchProductsFlow(query: String): Flow<List<Product>> {
        return productDao.searchProductsFlow(query).map { list -> list.map { it.toDomain() } }
    }

    suspend fun saveProduct(product: Product): Result<Unit> {
        val userId = preferenceManager.userId.firstOrNull()
        val initialStatus = if (userId != null) SyncStatus.PENDING_SYNC else SyncStatus.LOCAL_ONLY

        val entityToSave = ProductEntity.fromDomain(product.copy(syncStatus = initialStatus))
        productDao.insertOrUpdateProduct(entityToSave)

        // If warranty end date is present, configure standard reminders (30 days, 7 days, 1 day)
        if (product.warrantyEndMillis != null && product.warrantyEndMillis > System.currentTimeMillis()) {
            setupDefaultReminders(product)
        }

        // If bill document exists, record in documentDao
        if (!product.invoiceUri.isNullOrBlank()) {
            val docItem = DocumentEntity(
                id = UUID.randomUUID().toString(),
                productId = product.id,
                productName = product.name,
                localUri = product.invoiceUri,
                cloudUrl = product.invoiceCloudUrl,
                mimeType = if (product.invoiceUri.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg",
                fileName = "Bill_${product.name.replace(" ", "_")}.jpg",
                fileSize = 0L,
                createdAtMillis = System.currentTimeMillis()
            )
            documentDao.insertDocument(docItem)
        }

        // Attempt immediate cloud sync if authenticated
        if (userId != null) {
            val syncResult = firestoreSync.syncProductToCloud(userId, product)
            if (syncResult.isSuccess) {
                productDao.insertOrUpdateProduct(entityToSave.copy(syncStatus = SyncStatus.SYNCED))
            } else {
                productDao.insertOrUpdateProduct(entityToSave.copy(syncStatus = SyncStatus.SYNC_ERROR))
            }
        }

        return Result.success(Unit)
    }

    private suspend fun setupDefaultReminders(product: Product) {
        val expiryMillis = product.warrantyEndMillis ?: return
        val existing = reminderDao.getRemindersForProduct(product.id)
        if (existing.isNotEmpty()) return // Keep user's custom preferences if already configured

        val remindersToCreate = listOf(
            Triple(ReminderType.EXPIRY_30_DAYS, 30, "Warranty expires in 30 days"),
            Triple(ReminderType.EXPIRY_7_DAYS, 7, "Warranty expires in 7 days!"),
            Triple(ReminderType.EXPIRY_1_DAY, 1, "Warranty expires tomorrow! Check for repairs.")
        )

        val newEntities = mutableListOf<ReminderEntity>()
        for ((type, days, note) in remindersToCreate) {
            val triggerTime = DateUtils.calculateReminderTime(expiryMillis, days)
            if (triggerTime > System.currentTimeMillis()) {
                val reminder = Reminder(
                    id = UUID.randomUUID().toString(),
                    productId = product.id,
                    productName = product.name,
                    reminderType = type,
                    targetTimeMillis = triggerTime,
                    isEnabled = true,
                    isTriggered = false,
                    customNote = note
                )
                newEntities.add(ReminderEntity.fromDomain(reminder))
                alarmScheduler.scheduleReminder(reminder)
            }
        }
        if (newEntities.isNotEmpty()) {
            reminderDao.insertOrUpdateAll(newEntities)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val existingReminders = reminderDao.getRemindersForProduct(productId)
        for (r in existingReminders) {
            alarmScheduler.cancelReminder(r.id)
        }
        reminderDao.deleteRemindersForProduct(productId)
        documentDao.deleteDocumentsForProduct(productId)
        productDao.deleteProductById(productId)

        val userId = preferenceManager.userId.firstOrNull()
        if (userId != null) {
            firestoreSync.deleteProductFromCloud(userId, productId)
        }
        return Result.success(Unit)
    }

    suspend fun syncAllWithCloud(): Result<Int> {
        val userId = preferenceManager.userId.firstOrNull()
            ?: return Result.failure(Exception("Sign in required for cloud backup"))

        // 1. Upload local pending items
        val localProducts = productDao.getAllProducts()
        var syncedCount = 0
        for (p in localProducts) {
            val domain = p.toDomain()
            val result = firestoreSync.syncProductToCloud(userId, domain)
            if (result.isSuccess) {
                productDao.insertOrUpdateProduct(p.copy(syncStatus = SyncStatus.SYNCED))
                syncedCount++
            }
        }

        // 2. Download any remote products not present locally
        val remoteResult = firestoreSync.fetchProductsFromCloud(userId)
        if (remoteResult.isSuccess) {
            val remoteProducts = remoteResult.getOrNull() ?: emptyList()
            for (rp in remoteProducts) {
                val exists = productDao.getProductById(rp.id)
                if (exists == null) {
                    productDao.insertOrUpdateProduct(ProductEntity.fromDomain(rp))
                    syncedCount++
                }
            }
        }

        preferenceManager.setLastSyncMillis(System.currentTimeMillis())
        return Result.success(syncedCount)
    }

    suspend fun clearLocalData() {
        val reminders = reminderDao.getActivePendingReminders(0L)
        for (r in reminders) {
            alarmScheduler.cancelReminder(r.id)
        }
        reminderDao.clearAllReminders()
        documentDao.clearAllDocuments()
        productDao.clearAllProducts()
    }
}
