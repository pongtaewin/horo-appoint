package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.ThaiFormatter
import com.firebaseapp.horoappoint.entity.Appointment
import com.firebaseapp.horoappoint.entity.Timeframe
import org.springframework.data.repository.CrudRepository
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.math.min

private const val HOURS_BEFORE_BOOKING = 4L

private const val HOURS_PER_DAY = 24
private const val MINUTES_PER_HOUR = 60
private const val SECONDS_PER_MINUTE = 60
private const val FRAME_LENGTH_SECONDS = 15 * SECONDS_PER_MINUTE
private const val FRAME_AMOUNT = SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY / FRAME_LENGTH_SECONDS // 96
private const val MONTHLY_DISPLAY_DAYS = 42

interface TimeframeRepository : CrudRepository<Timeframe, Long> {

    fun findByAppointment(appointment: Appointment): Optional<Timeframe>

    fun getAvailableFrameForDate(date: LocalDate, now: LocalDateTime): Array<Boolean> {
        val st = date.atStartOfDay(ThaiFormatter.zoneId).toInstant().epochSecond

        fun idx(i: Instant) = (i.epochSecond - st).toInt()
            .let { (it / FRAME_LENGTH_SECONDS) - if (it % FRAME_LENGTH_SECONDS != 0) 1 else 0 }

        return Array(FRAME_AMOUNT) { true }.apply {
            findOccupiedTimeframeForDate(date).forEach { tf ->
                fill(false, idx(tf.startTime!!), idx(tf.endTime!!))
            }
            if (date == now.toLocalDate()) {
                val duration = Duration.between(now.toLocalDate().atStartOfDay(), now)
                    .plus(HOURS_BEFORE_BOOKING, ChronoUnit.HOURS)
                fill(
                    false,
                    0,
                    min(
                        (duration.toSeconds().toInt() + FRAME_LENGTH_SECONDS - 1) / FRAME_LENGTH_SECONDS,
                        FRAME_AMOUNT
                    )
                )
            }
        }
    }

    fun findOccupiedTimeframeForDate(date: LocalDate): List<Timeframe> {
        return findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            date.atStartOfDay(ThaiFormatter.zoneId).toInstant(),
            date.atStartOfDay(ThaiFormatter.zoneId).plus(1L, ChronoUnit.DAYS).toInstant()
        )
    }

    fun isRangeOccupied(startTime: Instant, endTime: Instant): Boolean =
        existsByStartTimeLessThanAndEndTimeGreaterThan(endTime, startTime)

    // @Query("select (count(t) > 0) from Timeframe t where t.startTime < ?1 and t.endTime > ?2")
    fun existsByStartTimeLessThanAndEndTimeGreaterThan(endTime: Instant, startTime: Instant): Boolean

    fun findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(startTime: Instant, endTime: Instant): List<Timeframe>

    fun findTimeInDay(date: LocalDate): List<Timeframe> {
        val start = date.atStartOfDay(ThaiFormatter.zoneId)
        val end = date.plusDays(1).atStartOfDay(ThaiFormatter.zoneId)
        val list = findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(start.toInstant(), end.toInstant())
        return list
    }

    fun findTimeInMonth(yearMonth: YearMonth): Pair<List<Int>, List<List<Timeframe>>> {
        val start = yearMonth.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(MONTHLY_DISPLAY_DAYS.toLong())
        val list = findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            start.atStartOfDay(ThaiFormatter.zoneId).toInstant(),
            end.atStartOfDay(ThaiFormatter.zoneId).toInstant()
        )
        println("Find Time in Month")
        println(start)
        println(end)
        println(list)

        return List(MONTHLY_DISPLAY_DAYS) { start.plusDays(it.toLong()).dayOfMonth } to
            list.groupBy {
                ChronoUnit.DAYS.between(
                    start,
                    ZonedDateTime.ofInstant(it.paddedStartTime, ThaiFormatter.zoneId).toLocalDate()
                ).toInt()
            }.let { m -> List(MONTHLY_DISPLAY_DAYS) { m[it] ?: listOf() } }
    }
}
