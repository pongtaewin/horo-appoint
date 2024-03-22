package com.firebaseapp.horoappoint.settings

import java.text.DecimalFormat
import java.time.*
import java.time.chrono.ThaiBuddhistChronology
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.*

class ThaiFormatter {

    companion object {
        val zoneId = ZoneId.of("Asia/Bangkok")
        fun of(pattern: String): DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
            .withLocale(Locale("th")).withChronology(ThaiBuddhistChronology.INSTANCE)

        fun now() = ZonedDateTime.now(zoneId)

        fun asZone(instant: Instant): ZonedDateTime = instant.atZone(zoneId)
        fun currency(amount: Double, requiresDecimal: Boolean = false): String =
            DecimalFormat(if (requiresDecimal || amount % 1.0 != 0.0) "#,##0.00" else "#,##0").format(amount)

        fun format(temporal: TemporalAccessor, pattern: String): String =
            DateTimeFormatter.ofPattern(pattern)
                .withLocale(Locale("th"))
                .withChronology(ThaiBuddhistChronology.INSTANCE)
                .format(temporal)


        fun durationDaysLeft(present: LocalDate, future: LocalDate): String {
            if (present == future) return "วันนี้"
            if (present.plusDays(1L) == future) return "วันพรุ่งนี้"

            val year = future.year - present.year - if (future < present.withYear(future.year)) 1 else 0
            val month = future.month.value - present.month.value +
                    if (future < present.withYear(future.year)) 12 else 0 - if (future.dayOfMonth < present.dayOfMonth) 1 else 0
            val day = Duration.between(
                present.atStartOfDay().plusMonths(month.toLong()).plusYears(year.toLong()),
                future.atStartOfDay()
            ).toDays().toInt()

            return buildString {
                append("อีก")
                if (year > 0) append(" $year ปี")
                if (month > 0) append(" $month เดือน")
                if (day >= 7) append(" ${day / 7} สัปดาห์")
                if (day % 7 != 0) append(" ${day % 7} วัน")
            }
        }
    }
}