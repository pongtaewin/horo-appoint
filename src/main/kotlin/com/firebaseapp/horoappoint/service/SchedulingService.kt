package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.model.CustomerSelection
import com.firebaseapp.horoappoint.repository.TimeframeRepository
import com.firebaseapp.horoappoint.settings.ThaiFormatter
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class SchedulingService(
    val messageService: MessageService,
    val timeframeRepository: TimeframeRepository
) {


    // todo document hardcoding time to be in range
    //     08:00-08:15 (frame 24) to 23:45-24:00 (frame 95)
    fun getSchedulingMessageModel(selection: CustomerSelection, date: LocalDate): ModelMap {
        val now = ThaiFormatter.now()
        return ModelMap().apply {
            addAttribute("service", buildMap {
                put("name", selection.service!!.name!!)
                put("price", ThaiFormatter.currency(selection.price!!))
                put("duration", selection.service!!.getDurationText())
                put("location", "ผ่านทางแชทไลน์") //todo add proper logic
                put("desc", selection.service!!.description!! /*?: "-"*/) //todo Check if mandatory
            })

            addAttribute("date", buildMap {
                put("dow", "${ThaiFormatter.format(date, "EEEE")}ที่")
                put("day", ThaiFormatter.format(date, "d MMMM yyyy"))
                put("rel", ThaiFormatter.durationDaysLeft(now.toLocalDate(), date))
                put("full_day", ThaiFormatter.format(date, "EEEEที่ d MMMM yyyy"))
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val min = now.toLocalDate()
                val max = min.plusMonths(6)
                val initial = if(min <= date && date <= max) date else min
                put("change", buildMap {
                    put("initial", formatter.format(initial))
                    put("min", formatter.format(min))
                    put("max", formatter.format(max))
                })
            })

            addAttribute("updated", ThaiFormatter.format(now, "d MMMM yyyy HH:mm:ss"))

            val frames = timeframeRepository.getAvailableFrameForDate(date)

            addAttribute("rows", lists.map { (label, range) ->
                buildMap {
                    put("label", label)
                    put("times", (range step 4).map { it.asTimeText() })
                    put("bar", range.chunked(4).map { it.map(frames::get) })
                }
            })

            val frameLength = selection.service!!.durationMinutes!!.let { it / 15 + if (it % 15 != 0) 1 else 0 }
            val framesSlot = frames.asSequence()
                .windowed(frameLength) { l -> l.reduce(Boolean::and) }.toList().toTypedArray()
            addAttribute("blocks", lists.map { (label, range) ->
                buildMap {
                    put("label", label)
                    put("rows", range.chunked(4).map { r ->
                        buildMap {
                            put("time", r.map { frameRangeAsText(it, frameLength) ?: "---" })
                            put("frame", r.map { it.toString() })
                            put("free", r.map { framesSlot.getOrElse(it) { false } })
                        }
                    })
                }
            })


        }
    }


    fun sendSchedulingMessage(event: ReplyEvent, selection: CustomerSelection, date: LocalDate) {
        sendSchedulingMessage(event, getSchedulingMessageModel(selection, date))
    }

    fun sendSchedulingMessage(event: ReplyEvent, modelMap: ModelMap) {
        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/time_checker.txt",
                modelMap,
                "กรุณาเลือกเวลารับบริการที่ต้องการ"
            )
        )
    }


    companion object {
        val lists = listOf(
            "ช่วงเช้า" to 32..47,
            "ช่วงสาย" to 48..63,
            "ช่วงเย็น" to 64..79,
            "ช่วงค่ำ" to 80..95,
        )

        fun Int.asTimeText(): String = if (this == 96) "24:00"
        else ThaiFormatter.format(LocalTime.ofSecondOfDay(this * 900L), "H:mm")

        private fun Int.frameTime(): LocalTime = LocalTime.ofSecondOfDay(this * 900L)
        fun frameRangeAsText(start: Int, range: Int): String? =
            when {
                start + range > 96 -> null
                start + range == 96 -> "${ThaiFormatter.format(start.frameTime(), "H:mm")} - 24:00 น."
                else -> "${ThaiFormatter.format(start.frameTime(), "H:mm")} - ${
                    ThaiFormatter.format((start + range).frameTime(), "H:mm")
                } น."
            }
    }

}