package com.warrantywallet.app

import com.warrantywallet.app.core.model.WarrantyStatus
import com.warrantywallet.app.core.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun testLeapYearCalculation() {
        // 2024 is a leap year. Feb 29 exists.
        val jan31Leap = DateUtils.localDateToMillis(LocalDate.of(2024, 1, 31))
        val febEndLeap = DateUtils.calculateWarrantyEndDate(jan31Leap, 1)
        val expectedDateLeap = LocalDate.of(2024, 2, 29)
        assertEquals(expectedDateLeap, DateUtils.millisToLocalDate(febEndLeap))

        // 2023 is not a leap year. Feb 28 exists.
        val jan31NonLeap = DateUtils.localDateToMillis(LocalDate.of(2023, 1, 31))
        val febEndNonLeap = DateUtils.calculateWarrantyEndDate(jan31NonLeap, 1)
        val expectedDateNonLeap = LocalDate.of(2023, 2, 28)
        assertEquals(expectedDateNonLeap, DateUtils.millisToLocalDate(febEndNonLeap))
    }

    @Test
    fun testOneYearWarrantyCalculation() {
        val start = DateUtils.localDateToMillis(LocalDate.of(2024, 5, 15))
        val end = DateUtils.calculateWarrantyEndDate(start, 12)
        val expected = LocalDate.of(2025, 5, 15)
        assertEquals(expected, DateUtils.millisToLocalDate(end))
    }

    @Test
    fun testWarrantyStatusResolution() {
        val fixedNow = DateUtils.localDateToMillis(LocalDate.of(2024, 6, 1))

        // Null expiry date -> UNKNOWN
        assertEquals(WarrantyStatus.UNKNOWN, DateUtils.getWarrantyStatus(null, fixedNow))

        // Past expiry date -> EXPIRED
        val expiredMillis = DateUtils.localDateToMillis(LocalDate.of(2024, 5, 30))
        assertEquals(WarrantyStatus.EXPIRED, DateUtils.getWarrantyStatus(expiredMillis, fixedNow))

        // Expires in 10 days (< 30 days) -> EXPIRING_SOON
        val expiringMillis = DateUtils.localDateToMillis(LocalDate.of(2024, 6, 11))
        assertEquals(WarrantyStatus.EXPIRING_SOON, DateUtils.getWarrantyStatus(expiringMillis, fixedNow))

        // Expires in 60 days (> 30 days) -> ACTIVE
        val activeMillis = DateUtils.localDateToMillis(LocalDate.of(2024, 8, 1))
        assertEquals(WarrantyStatus.ACTIVE, DateUtils.getWarrantyStatus(activeMillis, fixedNow))
    }

    @Test
    fun testDaysRemainingCalculations() {
        val fixedNow = DateUtils.localDateToMillis(LocalDate.of(2024, 10, 10))

        assertNull(DateUtils.getDaysRemaining(null, fixedNow))

        val expiry5Days = DateUtils.localDateToMillis(LocalDate.of(2024, 10, 15))
        assertEquals(5L, DateUtils.getDaysRemaining(expiry5Days, fixedNow))

        val todayExpiry = DateUtils.localDateToMillis(LocalDate.of(2024, 10, 10))
        assertEquals(0L, DateUtils.getDaysRemaining(todayExpiry, fixedNow))

        val pastExpiry = DateUtils.localDateToMillis(LocalDate.of(2024, 10, 5))
        assertEquals(-5L, DateUtils.getDaysRemaining(pastExpiry, fixedNow))
    }

    @Test
    fun testRemainingTextFormatting() {
        val fixedNow = DateUtils.localDateToMillis(LocalDate.of(2024, 10, 10))

        assertEquals("Warranty date not added", DateUtils.formatRemainingText(null, fixedNow))

        val todayExpiry = DateUtils.localDateToMillis(LocalDate.of(2024, 10, 10))
        assertEquals("Expires today", DateUtils.formatRemainingText(todayExpiry, fixedNow))

        val oneDay = DateUtils.localDateToMillis(LocalDate.of(2024, 10, 11))
        assertEquals("1 day remaining", DateUtils.formatRemainingText(oneDay, fixedNow))

        val fiveDays = DateUtils.localDateToMillis(LocalDate.of(2024, 10, 15))
        assertEquals("5 days remaining", DateUtils.formatRemainingText(fiveDays, fixedNow))

        val past = DateUtils.localDateToMillis(LocalDate.of(2024, 10, 7))
        assertEquals("Expired 3 days ago", DateUtils.formatRemainingText(past, fixedNow))
    }
}
