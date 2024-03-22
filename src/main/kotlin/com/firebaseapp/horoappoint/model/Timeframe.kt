package com.firebaseapp.horoappoint.model

import com.firebaseapp.horoappoint.settings.ThaiFormatter
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

    fun getStart() = ThaiFormatter.asZone(startTime!!)
    fun getEnd() = ThaiFormatter.asZone(endTime!!)

    //todo handle 24:00 case
    fun getCombinedDate(): String {
        val startDay = getStart().truncatedTo(ChronoUnit.DAYS)
        val endDay = getEnd().truncatedTo(ChronoUnit.DAYS)
        return when {
            startDay == endDay ||
                    startDay.plusDays(1) == endDay
                    && getEnd().run { hour == 0 && minute == 0 && second == 0 } ->
                ThaiFormatter.format(startDay, "EEEEที่ d MMMM G yyyy")

            startDay.year != endDay.year ->
                ThaiFormatter.format(startDay, "EE d MMM yyyy") + " ถึง " +
                        ThaiFormatter.format(endDay, "EE d MMM yyyy")

            startDay.month != endDay.month ->
                ThaiFormatter.format(startDay, "EEEEที่ d MMMM") + " ถึง" +
                        ThaiFormatter.format(endDay, "EEEEที่ d MMMM yyyy")

            else -> ThaiFormatter.format(startDay, "EEEEที่ d") + " ถึง" +
                    ThaiFormatter.format(endDay, "EEEEที่ d MMMM G yyyy")
        }
    }

    fun getCombinedTime(): String {
        val startDay = getStart().truncatedTo(ChronoUnit.DAYS)
        val endDay = getEnd().truncatedTo(ChronoUnit.DAYS)
        return when {
            startDay == endDay -> ThaiFormatter.format(getStart(), "H:mm") + " - " +
                    ThaiFormatter.format(getEnd(), "H:mm") + " น."

            startDay.plusDays(1) == endDay && getEnd().run { hour == 0 && minute == 0 && second == 0 } ->
                ThaiFormatter.format(getStart(), "H:mm") + " - 24:00 น."

            else -> String.format(
                "%s น. (%s) - %s น. (%s)",
                ThaiFormatter.format(getStart(), "H:mm"),
                ThaiFormatter.format(startDay, "d MMM"),
                ThaiFormatter.format(getEnd(), "H:mm"),
                ThaiFormatter.format(endDay, "d MMM")
            )
        }
    }

}