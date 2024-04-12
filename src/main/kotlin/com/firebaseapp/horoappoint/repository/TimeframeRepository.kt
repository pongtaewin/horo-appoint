package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.entity.Appointment
import com.firebaseapp.horoappoint.entity.Timeframe
import com.firebaseapp.horoappoint.settings.ThaiFormatter
import org.springframework.data.repository.CrudRepository
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

interface TimeframeRepository : CrudRepository<Timeframe, Long> {


    fun findByAppointment(appointment: Appointment): Optional<Timeframe>

    //todo manage logic [critical]
    //todo add time logic for same day case
    fun getAvailableFrameForDate(date: LocalDate): Array<Boolean> {
        val st = date.atStartOfDay(ThaiFormatter.zoneId).toInstant().epochSecond

        fun idx(i: Instant) = (i.epochSecond - st).toInt().let { (it / 900) - if (it % 900 != 0) 1 else 0 }


        return Array(96) { true }.apply {
            findOccupiedTimeframeForDate(date).forEach { tf ->
                fill(false, idx(tf.startTime!!), idx(tf.endTime!!))
            }
        }
    }


    fun findOccupiedTimeframeForDate(date: LocalDate): List<Timeframe> {
        return findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            date.atStartOfDay(ThaiFormatter.zoneId).toInstant(),
            date.atStartOfDay(ThaiFormatter.zoneId).plus(1L,ChronoUnit.DAYS).toInstant()
        )
    }

    fun isTimeframeOccupied(timeframe: Timeframe): Boolean {
        return isRangeOccupied(timeframe.paddedStartTime!!, timeframe.paddedEndTime!!)
    }

    fun isRangeOccupied(startTime: Instant, endTime: Instant): Boolean =
        existsByStartTimeLessThanAndEndTimeGreaterThan(endTime, startTime)

    //@Query("select (count(t) > 0) from Timeframe t where t.startTime < ?1 and t.endTime > ?2")
    fun existsByStartTimeLessThanAndEndTimeGreaterThan(endTime: Instant, startTime: Instant): Boolean

    fun findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(startTime: Instant, endTime: Instant): List<Timeframe>

}