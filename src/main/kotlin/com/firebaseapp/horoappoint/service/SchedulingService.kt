package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.ThaiFormatter
import com.firebaseapp.horoappoint.entity.Appointment
import com.firebaseapp.horoappoint.entity.ServiceType
import com.firebaseapp.horoappoint.entity.Timeframe
import com.firebaseapp.horoappoint.repository.AppointmentRepository
import com.firebaseapp.horoappoint.repository.CustomerRepository
import com.firebaseapp.horoappoint.repository.TimeframeRepository
import com.google.maps.GeoApiContext
import com.linecorp.bot.messaging.model.DatetimePickerAction
import com.linecorp.bot.messaging.model.QuickReply
import com.linecorp.bot.messaging.model.TextMessage
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.MessageEvent
import com.linecorp.bot.webhook.model.PostbackEvent
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

private const val HOURS_PER_DAY = 24
private const val MINUTES_PER_HOUR = 60
private const val SECONDS_PER_MINUTE = 60
private const val FRAME_LENGTH_SECONDS = 15 * SECONDS_PER_MINUTE
private const val FRAME_AMOUNT = SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY / FRAME_LENGTH_SECONDS // 96

private const val TIMETABLE_STEP_LENGTH = 4
private const val EIGHT_HOURS = 8
private const val DAILY_HOUR_OFFSET = 10
private const val SCHEDULE_DAYS = 180L
private const val DAY_TEMPLATE = "d MMMM yyyy"

@Suppress("TooManyFunctions", "LongParameterList")
@Service
class SchedulingService(
    @Value("\${gcp-maps.api-key}")
    private val apiKey: String,
    private val messageService: MessageService,
    private val appointmentRepository: AppointmentRepository,
    private val customerRepository: CustomerRepository,
    private val timeframeRepository: TimeframeRepository
) {

    val context: GeoApiContext = GeoApiContext.Builder().apiKey(apiKey).build()

    private val locIcon: String = "location.png"
    private val qrLocationEdit = messageService.quickReplyOf(locIcon, "เปลี่ยนตำแหน่ง", "location?overwrite=location")
    private val qrLocationEditChecked = { appointment: Appointment ->
        if (appointment.serviceChoice!!.serviceType!! == ServiceType.MEETUP) qrLocationEdit else null
    }
    private val qrDate = messageService.quickReplyOf("date.png", "เปลี่ยนวันที่/เวลา", "date?overwrite=date")
    private val qrServiceEdit = { appointment: Appointment ->
        messageService.quickReplyOf(
            "choice.png",
            "เปลี่ยนบริการ",
            "serviceChoice?id=${appointment.serviceChoice!!.service!!.id!!}"
        )
    }

    fun handleDateEvent(event: PostbackEvent, params: Map<String, String>) {
        val today = ThaiFormatter.now().toLocalDate()
        val appointment = appointmentRepository.findByEvent(event).get()

        if (params["overwrite"] == "date") {
            timeframeRepository.delete(appointment.timeframe!!)
            appointmentRepository.save(appointment.apply { timeframe = null })
        }

        messageService.replyMessage(
            event,
            if (params["occupied"] == "true") {
                val start = ThaiFormatter.asZone(
                    Instant.ofEpochSecond(
                        params["start"]?.toLong() ?: error("Null argument 'start'")
                    )
                )
                val end = ThaiFormatter.asZone(
                    Instant.ofEpochSecond(
                        params["end"]?.toLong() ?: error("Null argument 'end'")
                    )
                )
                val result = when (val type = params["type"]) {
                    "time" -> "วันที่ " + ThaiFormatter.format(start, "$DAY_TEMPLATE H:mm") +
                        " ถึง " + ThaiFormatter.format(end, "$DAY_TEMPLATE H:mm")

                    "date" -> "วันที่ " + ThaiFormatter.format(start, DAY_TEMPLATE) +
                        " ถึง " + ThaiFormatter.format(end, DAY_TEMPLATE)

                    else -> error("Illegal type '$type'")
                }
                TextMessage(
                    "ขออภัยครับ ในขณะนี้ช่วง\n$result\n" +
                        "ที่ได้เลือกไว้ไม่ว่างในระบบ\nกรุณาเลือกวันที่และ/หรือเวลาใหม่ครับ"
                )
            } else {
                null
            },
            messageService.processTemplateAndMakeMessage(
                "json/date.txt",
                ModelMap().apply {
                    put("service", appointment.serviceChoice!!.service!!.name)
                    put("choice", appointment.serviceChoice!!.name)
                    put("duration", appointment.serviceChoice!!.getDurationText())
                    putAll(getDateTimePicker(today, today))
                },
                "กรุณาเลือกเวลารับบริการ",
                qrServiceEdit(appointment),
                qrLocationEditChecked(appointment)
            )
        )
    }

    fun handleTimeEvent(event: PostbackEvent, params: Map<String, String>) {
        val now = ThaiFormatter.now()
        val date = LocalDate.parse(event.postback.params["date"] ?: error("Null argument 'date'"))
        val appointment = appointmentRepository.findByEvent(event).get()
        if (appointment.serviceChoice!!.durationDays?.let { it > 0 } == true) {
            return handleScheduleEvent(
                event,
                mapOf(
                    "frame" to date.atStartOfDay(ThaiFormatter.zoneId)
                        .plus(Duration.ofHours(EIGHT_HOURS.toLong())).toEpochSecond().toString()
                )
            )
        }
        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/scheduling.txt",
                getTimeModelMap(now, date, appointment),
                "กรุณาเลือกเวลารับบริการที่ต้องการ",
                qrServiceEdit(appointment),
                qrLocationEditChecked(appointment),
                getDateTimePicker(now.toLocalDate(), date).run {
                    messageService.quickReplyOf(
                        "date.png",
                        DatetimePickerAction(
                            "เปลี่ยนวันที่",
                            "time",
                            DatetimePickerAction.Mode.DATE,
                            get("initial")!!,
                            get("max")!!,
                            get("min")!!
                        )
                    )
                }
            )
        )
    }

    fun getTimeModelMap(now: ZonedDateTime, date: LocalDate, appointment: Appointment): ModelMap {
        val choice = appointment.serviceChoice!!
        val frameLength = minutesToFrame(choice.durationMinutes!!)
        val frames = timeframeRepository.getAvailableFrameForDate(date, now.toLocalDateTime())
        val frameSlot = frames.asSequence().windowed(frameLength) { l -> l.reduce(Boolean::and) }.toList()

        return ModelMap().apply {
            this["date"] = mapOf(
                "dow" to ThaiFormatter.format(date, "EEEEที่"),
                "day" to ThaiFormatter.format(date, DAY_TEMPLATE),
                "rel" to ThaiFormatter.durationDaysLeft(now.toLocalDate(), date),
                "full_day" to ThaiFormatter.format(date, "EEEEที่ $DAY_TEMPLATE")
            )

            this["choice"] = mapOf(
                "name" to choice.name!!,
                "desc" to choice.description!!.replace("\n", " "),
                "price" to ThaiFormatter.currency(choice.price!!),
                "location" to appointment.getLocationDescriptor(),
                "duration" to choice.getDurationText()
            )
            this["service"] = mapOf(
                "name" to choice.service!!.name!!,
                "desc" to choice.service!!.description!!.replace("\n", " ")
            )

            this["updated"] = ThaiFormatter.format(ThaiFormatter.now(), "$DAY_TEMPLATE HH:mm:ss")
            this["rows"] = lists.map { (label, range) ->
                mapOf(
                    "label" to label,
                    "times" to (range step TIMETABLE_STEP_LENGTH).map(::frameToText),
                    "bar" to range.chunked(TIMETABLE_STEP_LENGTH).map { it.map(frames::get) }
                )
            }
            this["blocks"] = lists.map { (label, range) ->
                mapOf(
                    "label" to label,
                    "rows" to range.chunked(TIMETABLE_STEP_LENGTH).map { r ->
                        mapOf<String, List<Any>>(
                            "stime" to r.map { f ->
                                if (f + frameLength <= FRAME_AMOUNT) frameToText(f) + " น." else "---"
                            },
                            "time" to r.map { f -> frameRangeToText(f..(f + frameLength)) ?: "---" },
                            "frame" to r.map { f -> frameToEpoch(date, f) },
                            "free" to r.map { f -> frameSlot.getOrNull(f) ?: false }
                        )
                    }
                )
            }
        }
    }

    fun handleScheduleEvent(event: PostbackEvent, params: Map<String, String>) {
        val appointment = appointmentRepository.findByEvent(event).get()
        val choice = appointment.serviceChoice!!
        val days = choice.durationDays?.toLong()

        val start = Instant.ofEpochSecond(params["frame"]?.toLong() ?: error("Null argument 'frame'"))
        val padStart = start.minus((choice.paddingBeforeMinutes ?: 0).toLong(), ChronoUnit.MINUTES)
        val end = if (days == null) {
            start.plus(choice.durationMinutes!!.toLong(), ChronoUnit.MINUTES)
        } else {
            start.plus((days - 1) * HOURS_PER_DAY + DAILY_HOUR_OFFSET, ChronoUnit.HOURS)
        }
        val padEnd = end.plus((choice.paddingAfterMinutes ?: 0).toLong(), ChronoUnit.MINUTES)

        if (timeframeRepository.isRangeOccupied(padStart, padEnd)) {
            return handleDateEvent(
                event,
                buildMap {
                    put("occupied", "true")
                    put("start", start.epochSecond.toString())
                    put("end", end.epochSecond.toString())
                    put("type", if (days == null) "time" else "date")
                }
            )
        }

        timeframeRepository.findByAppointment(appointment).ifPresent { timeframeRepository.delete(it) }

        appointmentRepository.save(
            appointment.apply {
                timeframe = timeframeRepository.save(
                    Timeframe().apply {
                        startTime = start
                        endTime = end
                        paddedStartTime = padStart
                        paddedEndTime = padEnd
                        approved = false
                        this.appointment = appointment
                        customer = customerRepository.findByEvent(event).get()
                    }
                )
            }
        )
        handleNameEvent(event, params)
    }

    fun handleNameEvent(event: PostbackEvent, params: Map<String, String>) {
        val appointment = appointmentRepository.findByEvent(event).get()
        if (params["overwrite"] == "name") customerRepository.save(appointment.customer!!.apply { fullName = null })
        if (appointment.customer!!.fullName != null) return handleConfirmEvent(event, mapOf())

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage("json/name.txt", ModelMap(), "กรุณาใส่ชื่อ-นามสกุล"),
            TextMessage(
                QuickReply(listOfNotNull(qrServiceEdit(appointment), qrLocationEditChecked(appointment), qrDate)),
                null,
                "[ระบบเปิดรับคำตอบ]\r\nกรุณาพิมพ์ชื่อ-นามสกุลด้านล่าง\r\nโดยไม่ต้องใส่คำนำหน้าชื่อ",
                null,
                null
            )
        )
        customerRepository.save(customerRepository.findByEvent(event).get().apply { state = "name" })
    }

    @Suppress("UnusedParameter")
    fun handleNameReceivedEvent(event: MessageEvent, params: Map<String, String>) {
        handleConfirmEvent(event, mapOf())
    }

    @Suppress("UnusedParameter")
    fun <T> handleConfirmEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val appointment = appointmentRepository.findByEvent(event).get()
        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/confirm.txt",
                ModelMap().addAppointmentDetails(appointment),
                "กรุณาตรวจสอบและยืนยันการจอง",
                qrServiceEdit(appointment),
                qrLocationEditChecked(appointment),
                qrDate,
                messageService.quickReplyOf("name.png", "เปลี่ยนชื่อ-สกุล", "name?overwrite=name")
            )
        )
    }

    companion object {
        val lists = listOf(
            "ช่วงเช้า" to 32..<48,
            "ช่วงบ่าย" to 48..<64,
            "ช่วงเย็น" to 64..<80,
            "ช่วงค่ำ" to 80..<96,
        )

        fun ModelMap.addAppointmentDetails(appointment: Appointment) = apply {
            this["service"] = appointment.serviceChoice!!.service!!.name!!
            this["choice"] = appointment.serviceChoice!!.name!!
            this["price"] = appointment.serviceChoice!!.getPriceRounded()
            this["location"] = appointment.getLocationDescriptor()
            this["date"] = appointment.timeframe!!.getCombinedDate()
            this["time"] = appointment.timeframe!!.getCombinedTime()
            this["customer"] = appointment.customer!!.fullName!!
        }
        fun frameRangeToText(range: IntRange): String? = if (0 <= range.first && range.last <= FRAME_AMOUNT) {
            String.format(Locale.ENGLISH, "%s - %s น.", frameToText(range.first), frameToText(range.last))
        } else {
            null
        }

        fun frameToText(frame: Int) = if (frame != FRAME_AMOUNT) {
            ThaiFormatter.format(ThaiFormatter.timeSeconds(frame * FRAME_LENGTH_SECONDS), "H:mm")
        } else {
            "24:00"
        }

        fun frameToEpoch(date: LocalDate, frame: Int) =
            date.atStartOfDay(ThaiFormatter.zoneId).plus(
                (frame * FRAME_LENGTH_SECONDS).toLong(),
                ChronoUnit.SECONDS
            ).toEpochSecond()

        fun minutesToFrame(minutes: Int) = (minutes * SECONDS_PER_MINUTE + 1) / FRAME_LENGTH_SECONDS

        fun getDateTimePicker(today: LocalDate, selectedDate: LocalDate = today) =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(Locale.US).let { f ->
                mapOf(
                    "initial" to f.format(
                        if (selectedDate in today..today.plus(SCHEDULE_DAYS, ChronoUnit.DAYS)) selectedDate else today
                    ),
                    "min" to f.format(today),
                    "max" to f.format(today.plus(SCHEDULE_DAYS, ChronoUnit.DAYS))
                )
            }
    }
}
