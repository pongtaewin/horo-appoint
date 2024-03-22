package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.model.Timeframe
import com.firebaseapp.horoappoint.settings.ThaiFormatter
import org.springframework.data.repository.CrudRepository
import java.time.Instant
import java.time.LocalDate

interface TimeframeRepository : CrudRepository<Timeframe, Long> {


    fun getAvailableFrameForDate(date: LocalDate): Array<Boolean> {
        val st = date.atStartOfDay(ThaiFormatter.zoneId).toInstant().epochSecond

        // As Timeframe cannot ever be overlapped, this can work with O(n) complexity.
        // 96 gaps, 15 minutes = 900 seconds gap
        return Array(96) { true }.apply {
            findOccupiedTimeframeForDate(date).forEach { tf ->
                fill(
                    element = true,
                    fromIndex = (tf.startTime!!.epochSecond - st).toInt()
                        .let { (it / 960) - if (it % 960 != 0) 1 else 0 },
                    toIndex = (tf.endTime!!.epochSecond - st).toInt()
                        .let { (it / 960) + if (it % 960 != 0) 1 else 0 })
            }
        }
    }


    fun findOccupiedTimeframeForDate(date: LocalDate): List<Timeframe> {
        return findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            date.atStartOfDay(ThaiFormatter.zoneId).toInstant(),
            date.plusDays(1L).atStartOfDay(ThaiFormatter.zoneId).toInstant()
        )
    }

    fun findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(startTime: Instant, endTime: Instant): List<Timeframe>

}