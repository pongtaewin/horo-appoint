package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.model.CustomerSelection
import com.firebaseapp.horoappoint.model.enums.SelectionState
import com.firebaseapp.horoappoint.model.enums.ServiceType
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
        /*if (selection.checkSelectionState() != SelectionState.SCHEDULE_REQUIRED)
            throw IllegalStateException("Scheduling Not Updated") //todo check logic*/

        val now = ThaiFormatter.now()
        val today = now.toLocalDate()
        return ModelMap().apply {
            val choice = selection.serviceChoice!!
            addAttribute("service", buildMap {
                put("name", choice.service!!.name!!)
                put("price", ThaiFormatter.currency(choice.price!!))
                put("duration", choice.getDurationText())
                put("location", selection.getLocationDescriptor())
                put("desc", choice.getFullDescription())
            })

            addAttribute("date", buildMap {
                put("dow", ThaiFormatter.format(date, "EEEEที่"))
                put("day", ThaiFormatter.format(date, "d MMMM yyyy"))
                put("rel", ThaiFormatter.durationDaysLeft(today, date))
                put("full_day", ThaiFormatter.format(date, "EEEEที่ d MMMM yyyy"))

                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                put("change", buildMap {
                    put("initial", formatter.format(if (date in today..(today.plusMonths(6))) date else today))
                    put("min", formatter.format(today))
                    put("max", formatter.format(today.plusMonths(6)))
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

            //todo deprecation
            val frameLength = 1//selection.service!!.durationMinutes!!.let { it / 15 + if (it % 15 != 0) 1 else 0 }
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
                "json/scheduling.txt",
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