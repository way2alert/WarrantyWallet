package com.warrantywallet.app.core.util

import com.warrantywallet.app.core.model.WarrantyStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {

    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val standardIsoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Converts epoch milliseconds to LocalDate in the system default timezone.
     */
    fun millisToLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    /**
     * Converts a LocalDate at start of day to epoch milliseconds.
     */
    fun localDateToMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Formats epoch milliseconds into a human-readable string (e.g. "24 Oct 2024").
     */
    fun formatDisplayDate(millis: Long?): String {
        if (millis == null || millis <= 0) return "Not specified"
        return displayDateFormatter.format(millisToLocalDate(millis))
    }

    /**
     * Calculates warranty end date given a start date and duration in months.
     * Handled accurately using Java 8+ java.time.LocalDate.plusMonths(), which
     * correctly handles month boundaries (e.g. Jan 31 + 1 month = Feb 28 or Feb 29 in leap year).
     */
    fun calculateWarrantyEndDate(startMillis: Long, durationMonths: Int): Long {
        if (durationMonths <= 0) return startMillis
        val startDate = millisToLocalDate(startMillis)
        val calculatedEnd = startDate.plusMonths(durationMonths.toLong())
        return localDateToMillis(calculatedEnd)
    }

    /**
     * Determines warranty status based on the expiry date.
     * Never guesses if the expiry date is null.
     */
    fun getWarrantyStatus(
        expiryMillis: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): WarrantyStatus {
        if (expiryMillis == null || expiryMillis <= 0) {
            return WarrantyStatus.UNKNOWN
        }

        val today = millisToLocalDate(nowMillis)
        val expiryDate = millisToLocalDate(expiryMillis)

        val daysRemaining = ChronoUnit.DAYS.between(today, expiryDate)

        return when {
            daysRemaining < 0 -> WarrantyStatus.EXPIRED
            daysRemaining <= 30 -> WarrantyStatus.EXPIRING_SOON
            else -> WarrantyStatus.ACTIVE
        }
    }

    /**
     * Returns days remaining until warranty expires.
     * Positive number = days until expiry.
     * 0 = expires today.
     * Negative number = days since expired.
     * null = expiry date unknown.
     */
    fun getDaysRemaining(
        expiryMillis: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (expiryMillis == null || expiryMillis <= 0) return null
        val today = millisToLocalDate(nowMillis)
        val expiryDate = millisToLocalDate(expiryMillis)
        return ChronoUnit.DAYS.between(today, expiryDate)
    }

    /**
     * Formats the remaining time in a user-friendly manner.
     * E.g. "24 days remaining", "Expires today", "Expired 12 days ago".
     */
    fun formatRemainingText(
        expiryMillis: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val days = getDaysRemaining(expiryMillis, nowMillis) ?: return "Warranty date not added"
        return when {
            days < 0 -> "Expired ${-days} days ago"
            days == 0L -> "Expires today"
            days == 1L -> "1 day remaining"
            else -> "$days days remaining"
        }
    }

    /**
     * Calculates reminder trigger timestamp.
     * e.g., 30 days before 9:00 AM on expiry date.
     */
    fun calculateReminderTime(expiryMillis: Long, daysBefore: Int, alertHour: Int = 9): Long {
        val expiryDate = millisToLocalDate(expiryMillis)
        val reminderDate = expiryDate.minusDays(daysBefore.toLong())
        return reminderDate.atTime(alertHour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
