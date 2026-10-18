package com.warrantywallet.app.core.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.warrantywallet.app.MainActivity
import com.warrantywallet.app.R
import com.warrantywallet.app.core.database.WarrantyWalletDatabase
import com.warrantywallet.app.core.model.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationHelper {

    const val CHANNEL_EXPIRY_ID = "warranty_expiry_channel"
    const val CHANNEL_MAINTENANCE_ID = "warranty_maintenance_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val expiryChannel = NotificationChannel(
                CHANNEL_EXPIRY_ID,
                "Warranty Expiry Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts before your product warranties end"
                enableVibration(true)
            }

            val maintenanceChannel = NotificationChannel(
                CHANNEL_MAINTENANCE_ID,
                "Service & Maintenance Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Routine maintenance and inspection reminders"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(expiryChannel)
            manager.createNotificationChannel(maintenanceChannel)
        }
    }

    fun showExpiryNotification(
        context: Context,
        notificationId: Int,
        productName: String,
        message: String,
        productId: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_PRODUCT_ID", productId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EXPIRY_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Warranty Alert: $productName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Handled when user revoked POST_NOTIFICATIONS permission
        }
    }
}

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(reminder: Reminder) {
        if (!reminder.isEnabled || reminder.targetTimeMillis <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, WarrantyNotificationReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.id)
            putExtra("PRODUCT_ID", reminder.productId)
            putExtra("PRODUCT_NAME", reminder.productName)
            putExtra("REMINDER_TITLE", reminder.reminderType.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.targetTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.targetTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.targetTimeMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminder.targetTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(reminderId: String) {
        val intent = Intent(context, WarrantyNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

class WarrantyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("REMINDER_ID") ?: return
        val productId = intent.getStringExtra("PRODUCT_ID") ?: ""
        val productName = intent.getStringExtra("PRODUCT_NAME") ?: "Product"
        val reminderTitle = intent.getStringExtra("REMINDER_TITLE") ?: "Warranty Alert"

        val message = "$productName: $reminderTitle. Check your warranty details and claim service if needed."

        NotificationHelper.showExpiryNotification(
            context = context,
            notificationId = reminderId.hashCode(),
            productName = productName,
            message = message,
            productId = productId
        )

        // Mark as triggered in database
        val db = WarrantyWalletDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminders = db.reminderDao().getRemindersForProduct(productId)
                val target = reminders.find { it.id == reminderId }
                if (target != null) {
                    db.reminderDao().insertOrUpdateReminder(target.copy(isTriggered = true))
                }
            } catch (_: Exception) {}
        }
    }
}

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON") {

            val scheduler = AlarmScheduler(context)
            val db = WarrantyWalletDatabase.getInstance(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val activeReminders = db.reminderDao().getActivePendingReminders(System.currentTimeMillis())
                    for (reminder in activeReminders) {
                        scheduler.scheduleReminder(reminder.toDomain())
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
