package com.firebaseapp.horoappoint.settings

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.ThaiBuddhistChronology
import java.time.format.DateTimeFormatter
import java.util.*

class ThaiDateTimeFormatters {

    companion object {
        fun of(pattern: String): DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
            .withLocale(Locale("th")).withChronology(ThaiBuddhistChronology.INSTANCE)

        fun withZone(instant: Instant): ZonedDateTime = instant.atZone(ZoneId.of("Asia/Bangkok"))
    }
}