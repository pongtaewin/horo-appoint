package com.firebaseapp.horoappoint

import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.ThaiBuddhistChronology
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.*

private const val DAYS_OF_WEEK = 7
private const val MONTHS_OF_YEAR = 12

object ThaiFormatter {

    val zoneId: ZoneId = ZoneId.of("Asia/Bangkok")

    fun now(): ZonedDateTime = ZonedDateTime.now(zoneId)

    fun timeSeconds(seconds: Int): LocalTime =
        now().toLocalDate().atStartOfDay().plusSeconds(seconds.toLong()).toLocalTime()
    fun asZone(instant: Instant): ZonedDateTime = instant.atZone(zoneId)
    fun currency(amount: Double, requiresDecimal: Boolean = false): String =
        DecimalFormat(if (requiresDecimal || amount % 1.0 != 0.0) "#,##0.00" else "#,##0").format(amount)

    fun format(temporal: TemporalAccessor, pattern: String): String =
        DateTimeFormatter.ofPattern(pattern)
            .withLocale(Locale("th"))
            .withChronology(ThaiBuddhistChronology.INSTANCE)
            .format(temporal)

    fun durationDaysLeft(present: LocalDate, future: LocalDate): String {
        when {
            present == future -> "วันนี้"
            present.plusDays(1L) == future -> "วันพรุ่งนี้"
            else -> null
        }?.let { return it }

        val year = future.year - present.year - if (future < present.withYear(future.year)) 1 else 0
        val month = future.month.value - present.month.value +
            (if (future < present.withYear(future.year)) MONTHS_OF_YEAR else 0) +
            (if (future.dayOfMonth < present.dayOfMonth) -1 else 0)

        val day = Duration.between(
            present.atStartOfDay().plusMonths(month.toLong()).plusYears(year.toLong()),
            future.atStartOfDay()
        ).toDays().toInt()

        return buildString {
            append("อีก")
            if (year > 0) append(" $year ปี")
            if (month > 0) append(" $month เดือน")
            if (day >= DAYS_OF_WEEK) append(" ${day / DAYS_OF_WEEK} สัปดาห์")
            if (day % DAYS_OF_WEEK != 0) append(" ${day % DAYS_OF_WEEK} วัน")
        }
    }
}
