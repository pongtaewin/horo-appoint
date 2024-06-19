package com.firebaseapp.horoappoint.entity

import com.firebaseapp.horoappoint.ThaiFormatter
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

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

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "padded_start_time", nullable = false)
    var paddedStartTime: Instant? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "padded_end_time", nullable = false)
    var paddedEndTime: Instant? = null

    @JdbcTypeCode(SqlTypes.BOOLEAN)
    @Column(name = "approved", nullable = false)
    var approved: Boolean? = false

    @OneToOne(orphanRemoval = false)
    @JoinColumn(name = "appointment_id")
    var appointment: Appointment? = null

    fun getStart() = ThaiFormatter.asZone(startTime!!)
    fun getEnd() = ThaiFormatter.asZone(endTime!!)

    @Suppress("CyclomaticComplexMethod")
    fun getCombinedDate(mini: Boolean = false): String {
        val startDay = getStart().truncatedTo(ChronoUnit.DAYS)
        val endDay = getEnd().truncatedTo(ChronoUnit.DAYS)
        val date = if (mini) "d MMM yy" else "EEEEที่ d MMMM yyyy"
        fun formatStart(startP: String) = ThaiFormatter.format(startDay, startP)
        fun formatEnd(endP: String) = (if (mini) " - " else " ถึง ") + ThaiFormatter.format(endDay, endP)
        fun String.deleteLastToken() = substringBeforeLast(" ", "")

        return when {
            startDay == endDay || startDay.plusDays(1) == endDay &&
                getEnd().run { hour == 0 && minute == 0 && second == 0 } ->
                formatStart(date)

            startDay.year != endDay.year ->
                formatStart(date.replace("EEEEที่", "EE")) + formatEnd(date.replace("EEEEที่", "EE"))

            startDay.month != endDay.month ->
                formatStart(date.deleteLastToken()) + formatEnd(date)

            else -> formatStart(date.deleteLastToken().deleteLastToken()) + formatEnd(date)
        }
    }

    fun getCombinedTime(mini: Boolean = false): String {
        val startDay = getStart().truncatedTo(ChronoUnit.DAYS)
        val endDay = getEnd().truncatedTo(ChronoUnit.DAYS)
        return when {
            startDay == endDay -> ThaiFormatter.format(getStart(), "H:mm") + " - " +
                ThaiFormatter.format(getEnd(), "H:mm") + if (mini) "" else " น."

            startDay.plusDays(1) == endDay && getEnd().run { hour == 0 && minute == 0 && second == 0 } ->
                ThaiFormatter.format(getStart(), "H:mm") + " - 24:00" + if (mini) "" else " น."

            else -> if (mini) {
                String.format(
                    Locale.ENGLISH,
                    "%s - %s",
                    ThaiFormatter.format(getStart(), "H:mm"),
                    ThaiFormatter.format(getEnd(), "H:mm"),
                )
            } else {
                String.format(
                    Locale.ENGLISH,
                    "%s น. (%s) - %s น. (%s)",
                    ThaiFormatter.format(getStart(), "H:mm"),
                    ThaiFormatter.format(startDay, "d MMM"),
                    ThaiFormatter.format(getEnd(), "H:mm"),
                    ThaiFormatter.format(endDay, "d MMM")
                )
            }
        }
    }
}
