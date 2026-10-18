package com.warrantywallet.app.core.database.converter

import androidx.room.TypeConverter
import com.warrantywallet.app.core.model.Category
import com.warrantywallet.app.core.model.ReminderType
import com.warrantywallet.app.core.model.SyncStatus

class Converters {
    @TypeConverter
    fun fromCategory(category: Category?): String {
        return category?.id ?: Category.OTHER.id
    }

    @TypeConverter
    fun toCategory(id: String?): Category {
        return Category.fromId(id)
    }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus?): String {
        return status?.name ?: SyncStatus.LOCAL_ONLY.name
    }

    @TypeConverter
    fun toSyncStatus(name: String?): SyncStatus {
        return try {
            if (name != null) SyncStatus.valueOf(name) else SyncStatus.LOCAL_ONLY
        } catch (_: Exception) {
            SyncStatus.LOCAL_ONLY
        }
    }

    @TypeConverter
    fun fromReminderType(type: ReminderType?): String {
        return type?.name ?: ReminderType.EXPIRY_30_DAYS.name
    }

    @TypeConverter
    fun toReminderType(name: String?): ReminderType {
        return try {
            if (name != null) ReminderType.valueOf(name) else ReminderType.EXPIRY_30_DAYS
        } catch (_: Exception) {
            ReminderType.EXPIRY_30_DAYS
        }
    }
}
