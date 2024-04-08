package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.model.CustomerSelection
import com.firebaseapp.horoappoint.repository.TimeframeRepository
import com.firebaseapp.horoappoint.settings.ThaiFormatter
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class SchedulingService(
    val messageService: MessageService,
    val timeframeRepository: TimeframeRepository
) {


    fun sendSchedulingMessage(event: ReplyEvent, selection: CustomerSelection, date: LocalDate) {
        val today = ThaiFormatter.now().toLocalDate()
        val frames = timeframeRepository.getAvailableFrameForDate(date)
        val choice = selection.serviceChoice!!
        val frameLength = minutesToFrame(choice.durationMinutes!!)
        val frameSlot = frames.asSequence().windowed(frameLength) { l -> l.reduce(Boolean::and) }
            .toList().toTypedArray<Boolean>()

        val model = ModelMap().apply {
            this["service"] = mapOf(
                "name" to choice.service!!.name!!,
                "duration" to choice.getDurationText(),
                "desc" to choice.getFullDescription(),
                "price" to ThaiFormatter.currency(choice.price!!),
                "location" to selection.getLocationDescriptor()
            )
            this["date"] = mapOf(
                "dow" to ThaiFormatter.format(date, "EEEEที่"),
                "day" to ThaiFormatter.format(date, "d MMMM yyyy"),
                "rel" to ThaiFormatter.durationDaysLeft(today, date),
                "full_day" to ThaiFormatter.format(date, "EEEEที่ d MMMM yyyy"),
                "change" to DateTimeFormatter.ofPattern("yyyy-MM-dd").let { f ->
                    "initial" to f.format(if (date in today..today.plusMonths(6)) date else today)
                    "min" to f.format(today)
                    "max" to f.format(today.plusMonths(6))
                }
            )
            this["updated"] = ThaiFormatter.format(ThaiFormatter.now(), "d MMMM yyyy HH:mm:ss")
            this["rows"] = lists.map { (label, range) ->
                mapOf(
                    "label" to label,
                    "times" to (range step 4).map(::frameToText),
                    "bar" to range.chunked(4).map { it.map(frames::get) }
                )
            }
            this["blocks"] = lists.map { (label, range) ->
                mapOf(
                    "label" to label,
                    "rows" to range.chunked(4).map { r ->
                        mapOf<String, List<Any>>(
                            "time" to r.map { frameRangeToText(it..<(it + frameLength)) ?: "---" },
                            "frame" to r.map { it.toString() },
                            "free" to r.map { frameSlot.getOrElse(it) { false } }
                        )
                    }
                )
            }
        }
        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/scheduling.txt", model, "กรุณาเลือกเวลารับบริการที่ต้องการ"
            )
        )
    }


    companion object {
        val lists = listOf(
            "ช่วงเช้า" to 32..<48,
            "ช่วงสาย" to 48..<64,
            "ช่วงเย็น" to 64..<80,
            "ช่วงค่ำ" to 80..<96,
        )

        fun frameRangeToText(range: IntRange): String? = if (range.first < 0 || range.last >= 96) null
        else String.format("%s - %s น.", frameToText(range.first), frameToText(range.last))

        fun frameToText(frame: Int) = String.format("%d:%02d", frame / 4, (frame % 4) * 15)
        fun minutesToFrame(minutes: Int) = (minutes / 15) + if (minutes % 15 != 0) 1 else 0
    }


   // val PARAMS: List<String> = TODO("Not yet implemented")

   /* fun <T> handleEvent(event: T, query: String, params: Map<String, String>) where T : Event, T : ReplyEvent {
        TODO("Not yet implemented")
    }

    */

}