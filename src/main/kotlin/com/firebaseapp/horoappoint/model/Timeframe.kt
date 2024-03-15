package com.firebaseapp.horoappoint.model

import com.firebaseapp.horoappoint.settings.ThaiDateTimeFormatters
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.temporal.ChronoUnit

@Entity
class Timeframe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timeframe_id", nullable = false)
    val id: Long? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "start_time", nullable = false)
    var startTime: Instant? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "end_time", nullable = false)
    var endTime: Instant? = null

    fun getStart() = ThaiDateTimeFormatters.withZone(startTime!!)
    fun getEnd() = ThaiDateTimeFormatters.withZone(endTime!!)

    //todo handle 24:00 case
    fun getCombinedDate(): String {
        val startDay = getStart().truncatedTo(ChronoUnit.DAYS)
        val endDay = getEnd().truncatedTo(ChronoUnit.DAYS)
        return when {
            startDay == endDay ||
                    startDay.plusDays(1) == endDay && getEnd().run { hour == 0 && minute == 0 && second == 0 }
            -> startDay.format(ThaiDateTimeFormatters.of("EEEEที่ d MMMM G yyyy"))

            startDay.year != endDay.year -> {
                startDay.format(ThaiDateTimeFormatters.of("EE d MMM yyyy")) + " ถึง " + endDay.format(
                    ThaiDateTimeFormatters.of("EE d MMM yyyy"))
            }

            startDay.month != endDay.month -> {
                startDay.format(ThaiDateTimeFormatters.of("EEEEที่ d MMMM")) + " ถึง" + endDay.format(
                    ThaiDateTimeFormatters.of("EEEEที่ d MMMM yyyy"))
            }

            else -> {
                return startDay.format(ThaiDateTimeFormatters.of("EEEEที่ d")) + " ถึง" + endDay.format(
                    ThaiDateTimeFormatters.of("EEEEที่ d MMMM G yyyy"))
            }
        }
    }

    fun getCombinedTime(): String {
        val startDay = getStart().truncatedTo(ChronoUnit.DAYS)
        val endDay = getEnd().truncatedTo(ChronoUnit.DAYS)
        return when {
            startDay == endDay -> {
                "${getStart().format(ThaiDateTimeFormatters.of("H:mm"))} - ${getEnd().format(ThaiDateTimeFormatters.of("H:mm"))} น."
            }

            startDay.plusDays(1) == endDay && getEnd().run { hour == 0 && minute == 0 && second == 0 } -> {
                "${getStart().format(ThaiDateTimeFormatters.of("H:mm"))} - 24:00 น."
            }

            else -> "${getStart().format(ThaiDateTimeFormatters.of("H:mm"))} น. (${startDay.format(
                ThaiDateTimeFormatters.of("d MMM"))})" +
                    " - ${getEnd().format(ThaiDateTimeFormatters.of("H:mm"))} น. (${endDay.format(ThaiDateTimeFormatters.of("d MMM"))})"
        }
    }
}