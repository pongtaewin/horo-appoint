package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.entity.Appointment
import com.firebaseapp.horoappoint.entity.Location
import com.firebaseapp.horoappoint.entity.ServiceType
import com.firebaseapp.horoappoint.entity.Timeframe
import com.firebaseapp.horoappoint.repository.*
import com.firebaseapp.horoappoint.settings.ThaiFormatter
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.model.AddressComponent
import com.google.maps.model.AddressComponentType
import com.google.maps.model.LatLng
import com.linecorp.bot.messaging.model.*
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.PostbackEvent
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import java.net.URI
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

@Service
class SchedulingService(
    @Value("\${gcp-maps.api-key}")
    private val apiKey: String,
    private val messageService: MessageService,
    private val appointmentRepository: AppointmentRepository,
    private val customerRepository: CustomerRepository,
    private val locationRepository: LocationRepository,
    private val serviceChoiceRepository: ServiceChoiceRepository,
    private val timeframeRepository: TimeframeRepository
) {

    val context: GeoApiContext = GeoApiContext.Builder().apiKey(apiKey).build()

    /*
    customer        Customer
    serviceChoice   ServiceChoice
    location        Location?
    timeframe       Timeframe?
    slipImage       URL?
    selectionAdded  Instant
    selectionFinal  Instant?
    slipAdded       Instant?
    slipFinal       Instant?
    approved        Instant?
     */

    fun <T> handleSelectEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val choice = serviceChoiceRepository.findById(params["choice"]!!.toLong()).get()
        val appointment =
            appointmentRepository.findByEvent(event).getOrNull()?.apply {
                serviceChoice = choice
            } ?: Appointment().apply {
                customer = customerRepository.findByEvent(event).get()
                serviceChoice = choice
                selectionAdded = ThaiFormatter.now().toInstant()
            }
        appointmentRepository.save(appointment)
        handleLocationEvent(event, params)
    }


    fun <T> handleLocationEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val appointment = appointmentRepository.findByEvent(event).get()
        if (params["overwrite"] == "location") appointmentRepository.save(appointment.apply { location = null })
        if (appointment.serviceChoice!!.serviceType!! != ServiceType.MEETUP) return handleDateEvent(event, params)
        else messageService.replyMessage(
            event, messageService.processTemplateAndMakeMessage(
                "json/location.txt", ModelMap(), "กรุณาเลือกตำแหน่งรับบริการ",
                QuickReplyItem(
                    URI("https://storage.googleapis.com/horo-appoint.appspot.com/location.png"),
                    LocationAction("เลือกตำแหน่ง")
                )
            )
        )
    }

    fun <T> handleLocationReceivedEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val location = mapCustomerLocation(params["lat"]!!.toDouble(), params["lon"]!!.toDouble(), context)
        appointmentRepository.save(appointmentRepository.findByEvent(event).get().apply {
            this.location = locationRepository.save(location)
        })
        handleLocationAddedEvent(event, params)
    }

    fun <T> handleLocationAddedEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val appointment = appointmentRepository.findByEvent(event).get()
        val location = appointment.location!!
        messageService.replyMessage(
            event, messageService.processTemplateAndMakeMessage(
                "json/location_added.txt", ModelMap().apply {
                    put("province", location.province!!)
                    put("district", location.district!!)
                    put("subdistrict", location.subdistrict!!)
                    put(
                        "km", DecimalFormat("#,##0.00")
                            .format(calculateDistanceFromBase(location.latitude!!, location.longitude!!))
                    )
                }, "กรุณาตรวจสอบตำแหน่งรับบริการ",

                QuickReplyItem(
                    URI("https://storage.googleapis.com/horo-appoint.appspot.com/choice.png"),
                    PostbackAction(
                        "เปลี่ยนบริการ",
                        "serviceChoice?id=${appointment.serviceChoice!!.service!!.id!!}",
                        "เปลี่ยนบริการ", null, null, null
                    )
                ),
                QuickReplyItem(
                    URI("https://storage.googleapis.com/horo-appoint.appspot.com/location.png"),
                    LocationAction("เลือกตำแหน่งใหม่")
                )
            )
        )
    }


    fun <T> handleDateEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val today = ThaiFormatter.now().toLocalDate()
        val appointment = appointmentRepository.findByEvent(event).get()

        if (params["overwrite"] == "date") {
            timeframeRepository.delete(appointment.timeframe!!)
            appointmentRepository.save(appointment.apply { timeframe = null })
        }

        messageService.replyMessage(
            event,
            *buildList {
                if (params["occupied"] == "true") {
                    val start = ThaiFormatter.asZone(Instant.ofEpochSecond(params["start"]!!.toLong()))
                    val end = ThaiFormatter.asZone(Instant.ofEpochSecond(params["end"]!!.toLong()))
                    val result = when (params["type"]!!) {
                        "time" -> "วันที่ " + ThaiFormatter.format(start, "d MMMM yyyy h:mm") +
                                " ถึง " + ThaiFormatter.format(end, "d MMMM yyyy h:mm")

                        "date" -> "วันที่ " + ThaiFormatter.format(start, "d MMMM yyyy") +
                                " ถึง " + ThaiFormatter.format(end, "d MMMM yyyy")

                        else -> IllegalArgumentException()
                    }
                    add(TextMessage("$result ไม่ว่างในระบบ กรุณาเลือกวันที่และ/หรือเวลาใหม่"))
                }
                add(
                    messageService.processTemplateAndMakeMessage(
                        "json/date.txt", ModelMap().apply {
                            put("service", appointment.serviceChoice!!.service!!.name)
                            put("choice", appointment.serviceChoice!!.name)
                            put("duration", appointment.serviceChoice!!.getDurationText())
                            putAll(getDateTimePicker(today, today))
                        }, "กรุณาเลือกเวลารับบริการ",
                        *buildList {
                            add(
                                QuickReplyItem(
                                    URI("https://storage.googleapis.com/horo-appoint.appspot.com/choice.png"),
                                    PostbackAction(
                                        "เปลี่ยนบริการ",
                                        "serviceChoice?id=${appointment.serviceChoice!!.service!!.id!!}",
                                        "เปลี่ยนบริการ", null, null, null
                                    )
                                )
                            )

                            if (appointment.serviceChoice!!.serviceType!! == ServiceType.MEETUP) add(
                                QuickReplyItem(
                                    URI("https://storage.googleapis.com/horo-appoint.appspot.com/location.png"),
                                    PostbackAction(
                                        "เปลี่ยนตำแหน่ง", "location?overwrite=location", "เปลี่ยนตำแหน่ง",
                                        null, null, null
                                    )
                                )
                            )
                        }.toTypedArray()
                    )
                )
            }.toTypedArray()
        )
    }

    fun <T> handleTimeEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val today = ThaiFormatter.now().toLocalDate()
        val appointment = appointmentRepository.findByEvent(event).get()
        val date = LocalDate.parse((event as PostbackEvent).postback.params["date"]!!)

        if (appointment.serviceChoice!!.durationDays?.let { it > 0 } == true) {
            return handleScheduleEvent(
                event,
                mapOf(
                    "frame" to date.atStartOfDay(ThaiFormatter.zoneId)
                        .plus(8L, ChronoUnit.HOURS).toEpochSecond().toString()
                )
            )
        }

        val choice = appointment.serviceChoice!!
        val frames = timeframeRepository.getAvailableFrameForDate(date)
        val frameLength = minutesToFrame(choice.durationMinutes!!)
        val frameSlot = frames.asSequence().windowed(frameLength) { l -> l.reduce(Boolean::and) }.toList()

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/scheduling.txt", ModelMap().apply {
                    put(
                        "date", mapOf(
                            "dow" to ThaiFormatter.format(date, "EEEEที่"),
                            "day" to ThaiFormatter.format(date, "d MMMM yyyy"),
                            "rel" to ThaiFormatter.durationDaysLeft(today, date),
                            "full_day" to ThaiFormatter.format(date, "EEEEที่ d MMMM yyyy")
                        )
                    )

                    put(
                        "choice", mapOf(
                            "name" to choice.name!!,
                            "desc" to choice.description!!.replace("\n"," "),
                            "price" to ThaiFormatter.currency(choice.price!!),
                            "location" to appointment.getLocationDescriptor(),
                            "duration" to choice.getDurationText()
                        )
                    )
                    put(
                        "service", mapOf(
                            "name" to choice.service!!.name!!,
                            "desc" to choice.service!!.description!!.replace("\n"," ")
                        )
                    )


                    put("updated", ThaiFormatter.format(ThaiFormatter.now(), "d MMMM yyyy HH:mm:ss"))
                    put("rows", lists.map { (label, range) ->
                        mapOf(
                            "label" to label,
                            "times" to (range step 4).map(::frameToText),
                            "bar" to range.chunked(4).map { it.map(frames::get) }
                        )
                    })
                    put("blocks", lists.map { (label, range) ->
                        mapOf(
                            "label" to label,
                            "rows" to range.chunked(4).map { r ->
                                mapOf<String, List<Any>>(
                                    "stime" to r.map { f -> if (f + frameLength <= 96) frameToText(f) + " น." else "---" },
                                    "time" to r.map { f -> frameRangeToText(f..(f + frameLength)) ?: "---" },
                                    "frame" to r.map { f -> frameToEpoch(date, f) },
                                    "free" to r.map { f -> frameSlot.getOrNull(f) ?: false }
                                )
                            }
                        )
                    })
                }, "กรุณาเลือกเวลารับบริการที่ต้องการ", *buildList {
                    add(
                        QuickReplyItem(
                            URI("https://storage.googleapis.com/horo-appoint.appspot.com/service.png"),
                            PostbackAction(
                                "เปลี่ยนบริการ",
                                "serviceChoice?id=${appointment.serviceChoice!!.service!!.id}",
                                "เปลี่ยนกลุ่มบริการ", null, null, null
                            )
                        )
                    )
                    if (appointment.serviceChoice!!.serviceType!! == ServiceType.MEETUP) add(
                        QuickReplyItem(
                            URI("https://storage.googleapis.com/horo-appoint.appspot.com/location.png"),
                            PostbackAction(
                                "เปลี่ยนตำแหน่ง", "location?overwrite=location", "เปลี่ยนตำแหน่ง",
                                null, null, null
                            )
                        )
                    )
                    with(getDateTimePicker(today, date)) {
                        add(
                            QuickReplyItem(
                                URI("https://storage.googleapis.com/horo-appoint.appspot.com/date.png"),
                                DatetimePickerAction(
                                    "เปลี่ยนวันที่", "time", DatetimePickerAction.Mode.DATE,
                                    get("initial")!!, get("max")!!, get("min")!!
                                )
                            )
                        )
                    }
                }.toTypedArray()
            )
        )
    }

    fun <T> handleScheduleEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val appointment = appointmentRepository.findByEvent(event).get()
        val choice = appointment.serviceChoice!!
        val days = choice.durationDays?.toLong()

        val start = Instant.ofEpochSecond(params["frame"]!!.toLong())
        val padStart = start.minus((choice.paddingBeforeMinutes ?: 0).toLong(), ChronoUnit.MINUTES)
        val end = if (days == null) start.plus(choice.durationMinutes!!.toLong(), ChronoUnit.MINUTES)
        else start.plus(days * 24 - 14, ChronoUnit.HOURS)
        val padEnd = end.plus((choice.paddingAfterMinutes ?: 0).toLong(), ChronoUnit.MINUTES)

        if (timeframeRepository.isRangeOccupied(padStart, padEnd))
            return handleDateEvent(event, buildMap {
                put("occupied", "true")
                put("start", start.epochSecond.toString())
                put("end", end.epochSecond.toString())
                put("type", if (days == null) "time" else "date")
            })

        timeframeRepository.findByAppointment(appointment).ifPresent { timeframeRepository.delete(it) }

        appointmentRepository.save(appointment.apply {
            timeframe = timeframeRepository.save(Timeframe().apply {
                startTime = start
                endTime = end
                paddedStartTime = padStart
                paddedEndTime = padEnd
                approved = false
                this.appointment = appointment
                customer = customerRepository.findByEvent(event).get()
            })
        })
        handleNameEvent(event, params)
    }

    fun <T> handleNameEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val appointment = appointmentRepository.findByEvent(event).get()

        if (params["overwrite"] == "name") customerRepository.save(appointment.customer!!.apply { fullName = null })

        if (appointment.customer!!.fullName != null) return handleConfirmEvent(event, mapOf())

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage("json/name.txt", ModelMap(), "กรุณาใส่ชื่อ-นามสกุล"),
            TextMessage(QuickReply(buildList {
                add(
                    QuickReplyItem(
                        URI("https://storage.googleapis.com/horo-appoint.appspot.com/service.png"),
                        PostbackAction(
                            "เปลี่ยนบริการ",
                            "serviceChoice?id=${appointment.serviceChoice!!.service!!.id}",
                            "เปลี่ยนกลุ่มบริการ", null, null, null
                        )
                    )
                )
                if (appointment.serviceChoice!!.serviceType!! == ServiceType.MEETUP) add(
                    QuickReplyItem(
                        URI("https://storage.googleapis.com/horo-appoint.appspot.com/location.png"),
                        PostbackAction(
                            "เปลี่ยนตำแหน่ง", "location?overwrite=location", "เปลี่ยนตำแหน่ง",
                            null, null, null
                        )
                    )
                )
                add(
                    QuickReplyItem(
                        URI("https://storage.googleapis.com/horo-appoint.appspot.com/date.png"),
                        PostbackAction(
                            "เปลี่ยนวันที่/เวลา", "date?overwrite=date", "เปลี่ยนวันที่/เวลา",
                            null, null, null
                        )
                    )
                )
            }), null, "[ระบบเปิดรับคำตอบ]\r\nกรุณาพิมพ์ชื่อ-นามสกุลด้านล่าง\r\nโดยไม่ต้องใส่คำนำหน้าชื่อ", null, null)
        )
        customerRepository.save(customerRepository.findByEvent(event).get().apply { state = "name" })
    }

    fun <T> handleNameReceivedEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        handleConfirmEvent(event, mapOf())
    }

    fun <T> handleConfirmEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val appointment = appointmentRepository.findByEvent(event).get()
        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/confirm.txt", ModelMap().apply {
                    put("service", appointment.serviceChoice!!.service!!.name!!)
                    put("choice", appointment.serviceChoice!!.name!!)
                    put("price", appointment.serviceChoice!!.getPriceRounded())
                    put("location", appointment.getLocationDescriptor())
                    put("date", appointment.timeframe!!.getCombinedDate())
                    put("time", appointment.timeframe!!.getCombinedTime())
                    put("customer", appointment.customer!!.fullName!!)
                },
                "กรุณาตรวจสอบและยืนยันการจอง",
                *buildList {
                    add(
                        QuickReplyItem(
                            URI("https://storage.googleapis.com/horo-appoint.appspot.com/service.png"),
                            PostbackAction(
                                "เปลี่ยนบริการ",
                                "serviceChoice?id=${appointment.serviceChoice!!.service!!.id}",
                                "เปลี่ยนกลุ่มบริการ", null, null, null
                            )
                        )
                    )
                    if (appointment.serviceChoice!!.serviceType!! == ServiceType.MEETUP) add(
                        QuickReplyItem(
                            URI("https://storage.googleapis.com/horo-appoint.appspot.com/location.png"),
                            PostbackAction(
                                "เปลี่ยนตำแหน่ง", "location?overwrite=location", "เปลี่ยนตำแหน่ง",
                                null, null, null
                            )
                        )
                    )
                    add(
                        QuickReplyItem(
                            URI("https://storage.googleapis.com/horo-appoint.appspot.com/date.png"),
                            PostbackAction(
                                "เปลี่ยนวันที่/เวลา", "date?overwrite=date", "เปลี่ยนวันที่/เวลา",
                                null, null, null
                            )
                        )
                    )
                    add(
                        QuickReplyItem(
                            URI("https://storage.googleapis.com/horo-appoint.appspot.com/name.png"),
                            PostbackAction(
                                "เปลี่ยนชื่อ-สกุล", "name?overwrite=name", "เปลี่ยนชื่อ-สกุล",
                                null, null, null
                            )
                        )
                    )
                }.toTypedArray()
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

        const val deg2rad: Double = PI / 180.0

        const val latA = 13.414733 * deg2rad
        const val lonA = 99.987572 * deg2rad
        fun frameRangeToText(range: IntRange): String? = if (range.first < 0 || range.last > 96) null
        else String.format("%s - %s น.", frameToText(range.first), frameToText(range.last))

        fun frameToText(frame: Int) = String.format("%d:%02d", frame / 4, (frame % 4) * 15)

        fun frameToEpoch(date: LocalDate, frame: Int) =
            date.atStartOfDay(ThaiFormatter.zoneId).plus(15L * frame, ChronoUnit.MINUTES).toEpochSecond()


        fun minutesToFrame(minutes: Int) = (minutes / 15) + if (minutes % 15 != 0) 1 else 0

        fun calculateDistanceFromBase(lat: Double, lon: Double): Double = 6371.0 * acos(
            sin(lat * deg2rad) * sin(latA) + cos(lat * deg2rad) * cos(latA) * cos(lonA - lon * deg2rad)
        )

        fun getDateTimePicker(today: LocalDate, selectedDate: LocalDate = today) =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(Locale.US).let { f ->
                mapOf(
                    "initial" to f.format(if (selectedDate in today..today.plusMonths(6)) selectedDate else today),
                    "min" to f.format(today),
                    "max" to f.format(today.plusMonths(6))
                )
            }

        //todo handle case where location can't be found

        private fun mapCustomerLocation(lat: Double, lon: Double, context: GeoApiContext): Location =
            GeocodingApi.reverseGeocode(context, LatLng(lat, lon)).language("th")
                .await().map { it.addressComponents.toList() }
                .firstNotNullOfOrNull(::getInformationIfComplete)!!.let { (prov, dist, subd) ->
                    Location().apply {
                        latitude = lat
                        longitude = lon
                        province = prov
                        district = dist
                        subdistrict = subd
                    }
                }

        private fun getInformationIfComplete(result: List<AddressComponent>): List<String>? =
            (result.find { AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1 in it.types })?.let { prov ->
                if ("กรุงเทพมหานคร" in prov.longName) {
                    val dist = result.find { AddressComponentType.SUBLOCALITY_LEVEL_1 in it.types } ?: return null
                    val subd = result.find { AddressComponentType.SUBLOCALITY_LEVEL_2 in it.types } ?: return null
                    listOf(
                        "กรุงเทพมหานคร",
                        "แขวง" + dist.longName.replace("แขวง", "").trim(),
                        "เขด" + subd.longName.replace("เขด", "").trim()
                    )
                } else {
                    val distA =
                        result.find { AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_2 in it.types } ?: return null
                    val subdA = result.find { AddressComponentType.LOCALITY in it.types } ?: return null
                    listOf(
                        "จังหวัด" + prov.longName.replace("จังหวัด", "").trim(),
                        "อำเภอ" + distA.longName.replace("อำเภอ", "").trim(),
                        "ตำบล" + subdA.longName.replace("ตำบล", "").trim()
                    )
                }
            }
    }

}