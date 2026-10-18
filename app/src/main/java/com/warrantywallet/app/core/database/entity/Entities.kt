package com.warrantywallet.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.warrantywallet.app.core.model.Category
import com.warrantywallet.app.core.model.DocumentItem
import com.warrantywallet.app.core.model.Product
import com.warrantywallet.app.core.model.Reminder
import com.warrantywallet.app.core.model.ReminderType
import com.warrantywallet.app.core.model.SyncStatus

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val brand: String?,
    val category: Category,
    val purchaseDateMillis: Long?,
    val warrantyStartMillis: Long?,
    val warrantyEndMillis: Long?,
    val warrantyDurationMonths: Int?,
    val seller: String?,
    val price: Double?,
    val currency: String,
    val serialNumber: String?,
    val modelNumber: String?,
    val invoiceUri: String?,
    val invoiceCloudUrl: String?,
    val notes: String?,
    val syncStatus: SyncStatus,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): Product = Product(
        id = id,
        name = name,
        brand = brand,
        category = category,
        purchaseDateMillis = purchaseDateMillis,
        warrantyStartMillis = warrantyStartMillis,
        warrantyEndMillis = warrantyEndMillis,
        warrantyDurationMonths = warrantyDurationMonths,
        seller = seller,
        price = price,
        currency = currency,
        serialNumber = serialNumber,
        modelNumber = modelNumber,
        invoiceUri = invoiceUri,
        invoiceCloudUrl = invoiceCloudUrl,
        notes = notes,
        syncStatus = syncStatus,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

    companion object {
        fun fromDomain(product: Product): ProductEntity = ProductEntity(
            id = product.id,
            name = product.name,
            brand = product.brand,
            category = product.category,
            purchaseDateMillis = product.purchaseDateMillis,
            warrantyStartMillis = product.warrantyStartMillis,
            warrantyEndMillis = product.warrantyEndMillis,
            warrantyDurationMonths = product.warrantyDurationMonths,
            seller = product.seller,
            price = product.price,
            currency = product.currency,
            serialNumber = product.serialNumber,
            modelNumber = product.modelNumber,
            invoiceUri = product.invoiceUri,
            invoiceCloudUrl = product.invoiceCloudUrl,
            notes = product.notes,
            syncStatus = product.syncStatus,
            createdAtMillis = product.createdAtMillis,
            updatedAtMillis = product.updatedAtMillis
        )
    }
}

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val productId: String,
    val productName: String,
    val reminderType: ReminderType,
    val targetTimeMillis: Long,
    val isEnabled: Boolean,
    val isTriggered: Boolean,
    val customNote: String?
) {
    fun toDomain(): Reminder = Reminder(
        id = id,
        productId = productId,
        productName = productName,
        reminderType = reminderType,
        targetTimeMillis = targetTimeMillis,
        isEnabled = isEnabled,
        isTriggered = isTriggered,
        customNote = customNote
    )

    companion object {
        fun fromDomain(reminder: Reminder): ReminderEntity = ReminderEntity(
            id = reminder.id,
            productId = reminder.productId,
            productName = reminder.productName,
            reminderType = reminder.reminderType,
            targetTimeMillis = reminder.targetTimeMillis,
            isEnabled = reminder.isEnabled,
            isTriggered = reminder.isTriggered,
            customNote = reminder.customNote
        )
    }
}

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val productId: String,
    val productName: String,
    val localUri: String,
    val cloudUrl: String?,
    val mimeType: String,
    val fileName: String,
    val fileSize: Long,
    val createdAtMillis: Long
) {
    fun toDomain(): DocumentItem = DocumentItem(
        id = id,
        productId = productId,
        productName = productName,
        localUri = localUri,
        cloudUrl = cloudUrl,
        mimeType = mimeType,
        fileName = fileName,
        fileSize = fileSize,
        createdAtMillis = createdAtMillis
    )

    companion object {
        fun fromDomain(doc: DocumentItem): DocumentEntity = DocumentEntity(
            id = doc.id,
            productId = doc.productId,
            productName = doc.productName,
            localUri = doc.localUri,
            cloudUrl = doc.cloudUrl,
            mimeType = doc.mimeType,
            fileName = doc.fileName,
            fileSize = doc.fileSize,
            createdAtMillis = doc.createdAtMillis
        )
    }
}
