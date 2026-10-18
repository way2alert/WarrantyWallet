package com.warrantywallet.app.core.model

data class ExtractedBillData(
    val productName: String? = null,
    val brand: String? = null,
    val seller: String? = null,
    val purchaseDateMillis: Long? = null,
    val price: Double? = null,
    val invoiceNumber: String? = null,
    val warrantyMonthsSuggested: Int? = null,
    val confidenceScore: Float = 0.0f,
    val rawExtractedLines: List<String> = emptyList()
)

data class Product(
    val id: String,
    val name: String,
    val brand: String? = null,
    val category: Category = Category.OTHER,
    val purchaseDateMillis: Long? = null,
    val warrantyStartMillis: Long? = null,
    val warrantyEndMillis: Long? = null,
    val warrantyDurationMonths: Int? = null,
    val seller: String? = null,
    val price: Double? = null,
    val currency: String = "INR",
    val serialNumber: String? = null,
    val modelNumber: String? = null,
    val invoiceUri: String? = null,
    val invoiceCloudUrl: String? = null,
    val notes: String? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class Reminder(
    val id: String,
    val productId: String,
    val productName: String,
    val reminderType: ReminderType,
    val targetTimeMillis: Long,
    val isEnabled: Boolean = true,
    val isTriggered: Boolean = false,
    val customNote: String? = null
)

enum class ReminderType(val title: String) {
    EXPIRY_30_DAYS("30 Days Before Expiry"),
    EXPIRY_7_DAYS("7 Days Before Expiry"),
    EXPIRY_1_DAY("1 Day Before Expiry"),
    MAINTENANCE("Service Check / Maintenance"),
    CUSTOM("Custom Date")
}

data class DocumentItem(
    val id: String,
    val productId: String,
    val productName: String,
    val localUri: String,
    val cloudUrl: String? = null,
    val mimeType: String = "image/jpeg",
    val fileName: String,
    val fileSize: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis()
)
