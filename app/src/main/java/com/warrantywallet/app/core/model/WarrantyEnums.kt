package com.warrantywallet.app.core.model

import androidx.compose.ui.graphics.Color

enum class WarrantyStatus(val displayName: String) {
    ACTIVE("Active"),
    EXPIRING_SOON("Expiring Soon"),
    EXPIRED("Expired"),
    UNKNOWN("Date Not Added");

    val badgeColor: Long
        get() = when (this) {
            ACTIVE -> 0xFF10B981 // Emerald Green
            EXPIRING_SOON -> 0xFFF59E0B // Amber Warning
            EXPIRED -> 0xFFEF4444 // Crimson Red
            UNKNOWN -> 0xFF6B7280 // Slate Gray
        }

    val containerColor: Long
        get() = when (this) {
            ACTIVE -> 0xFFD1FAE5
            EXPIRING_SOON -> 0xFFFEF3C7
            EXPIRED -> 0xFFFEE2E2
            UNKNOWN -> 0xFFF3F4F6
        }
}

enum class Category(val id: String, val displayName: String, val defaultWarrantyMonths: Int? = null) {
    ELECTRONICS("electronics", "Electronics", 12),
    APPLIANCES("appliances", "Home Appliances", 24),
    MOBILE("mobile", "Mobile & Tablets", 12),
    COMPUTERS("computers", "Computers & Laptops", 12),
    KITCHEN("kitchen", "Kitchen", 12),
    FURNITURE("furniture", "Furniture", 36),
    VEHICLES("vehicles", "Vehicles & Spares", 12),
    FASHION("fashion", "Watches & Wearables", 12),
    TOOLS("tools", "Tools & Equipment", 12),
    OTHER("other", "Other", null);

    companion object {
        fun fromId(id: String?): Category {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: OTHER
        }
    }
}

enum class SyncStatus {
    LOCAL_ONLY,
    PENDING_SYNC,
    SYNCED,
    SYNC_ERROR
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिंदी (Hindi)"),
    TAMIL("ta", "தமிழ் (Tamil)")
}
