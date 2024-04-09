package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.model.Customer
import com.firebaseapp.horoappoint.model.CustomerSelection
import com.firebaseapp.horoappoint.model.Location
import com.firebaseapp.horoappoint.model.ServiceChoice
import com.firebaseapp.horoappoint.model.enums.SelectionState
import com.firebaseapp.horoappoint.repository.CustomerRepository
import com.firebaseapp.horoappoint.repository.CustomerSelectionRepository
import com.firebaseapp.horoappoint.repository.ServiceChoiceRepository
import com.firebaseapp.horoappoint.repository.TimeframeRepository
import com.firebaseapp.horoappoint.settings.ThaiFormatter
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.model.AddressComponent
import com.google.maps.model.AddressComponentType
import com.google.maps.model.LatLng
import com.linecorp.bot.messaging.model.LocationAction
import com.linecorp.bot.messaging.model.Message
import com.linecorp.bot.messaging.model.PostbackAction
import com.linecorp.bot.messaging.model.QuickReplyItem
import com.linecorp.bot.messaging.model.TextMessage
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

@Service
class SchedulingService(
    val messageService: MessageService,
    val timeframeRepository: TimeframeRepository,
    private val serviceChoiceRepository: ServiceChoiceRepository,
    private val customerSelectionRepository: CustomerSelectionRepository,
    private val customerRepository: CustomerRepository,
    @Value("\${gcp-maps.api-key}") private val apiKey: String,
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

        const val SERVICE_SELECTED = "serviceSelected"
        const val NAME_RECEIVED = "nameReceived"
    }


    // val PARAMS: List<String> = TODO("Not yet implemented")

    /* fun <T> handleEvent(event: T, query: String, params: Map<String, String>) where T : Event, T : ReplyEvent {
         TODO("Not yet implemented")
     }

     */


    fun customerFromEvent(event: Event): Customer {
        return customerRepository.findByLineUID(event.source().userId()).get()
    }

    fun selectionFromEventOrNull(event: Event): CustomerSelection? {
        return customerSelectionRepository.findByCustomer(customerFromEvent(event)).getOrNull()
    }

    fun <T> handleServiceSelectedEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val choice = serviceChoiceRepository.findById(params["choice"]!!.toLong()).get()

        println("Service Selected = $choice")
        // Handle Switched State
        val selection = getOrCreateCustomerSelection(event, choice)
        val message: Message = when (selection.getSelectionState()) {
            SelectionState.LOCATION_REQUIRED ->
                messageService.processTemplateAndMakeMessage(
                    "json/prompt_customer_location.txt",
                    ModelMap(),
                    "กรุณาเลือกสถานที่ให้บริการ",
                    QuickReplyItem(LocationAction("เลือกตำแหน่งบนแผนที่"))
                )


            SelectionState.DATE_REQUIRED -> messageService.processTemplateAndMakeMessage(
                "json/pick_date.txt",
                ModelMap(),
                "กรุณาเลือกวันที่"
            )

            SelectionState.TIME_REQUIRED -> messageService.processTemplateAndMakeMessage(
                "json/pick_date.txt",
                ModelMap(),
                "กรุณาเลือกเวลารับบริการ"
            )

            SelectionState.CUSTOMER_NAME_REQUIRED -> messageService.processTemplateAndMakeMessage(
                "json/enter_name.txt",
                ModelMap(),
                "กรุณาเลือกเวลารับบริการ",
                QuickReplyItem(PostbackAction("เปลี่ยนวันรับบริการ", "data", null, null, null, null)),
                QuickReplyItem(PostbackAction("เปลี่ยนเวลารับบริการ", "data", null, null, null, null))
            )

            SelectionState.READY -> messageService.processTemplateAndMakeMessage(
                "json/confirmation.txt",
                ModelMap(),
                "กรุณายืนยันออเดอร์",
                QuickReplyItem(PostbackAction("เปลี่ยนบริการ", "data", null, null, null, null)),
                QuickReplyItem(PostbackAction("เปลี่ยนวันรับบริการ", "data", null, null, null, null)),
                QuickReplyItem(PostbackAction("เปลี่ยนเวลารับบริการ", "data", null, null, null, null)),
                QuickReplyItem(PostbackAction("เปลี่ยนชื่อ-นามสกุล", "data", null, null, null, null))
            )
        }

        messageService.replyMessage(event, message)

    }

    fun <T> handleLocationEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val selection = selectionFromEventOrNull(event) ?: getOrCreateCustomerSelection(
            event, serviceChoiceRepository.findById(params["choice"]!!.toLong()).get()
        )
        if (params["dropLocation"] == "true") customerSelectionRepository.save(selection.apply { location = null })
        if (selection.location != null) return handleDateEvent(event,params)
        else messageService.replyMessage(event, TextMessage("ต้องการข้อมูลตำแหน่ง"))
    }

    fun <T> handleDateEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        messageService.replyMessage(event, TextMessage("ต้องการข้อมูลวันที่"))
    }

    fun <T> getOrCreateCustomerSelection(
        event: T,
        choice: ServiceChoice
    ): CustomerSelection where T : Event, T : ReplyEvent {
        val customer = customerFromEvent(event)
        return customerSelectionRepository.findByCustomer(customer).getOrElse {
            customerSelectionRepository.save(CustomerSelection().apply {
                this.customer = customer
                serviceChoice = choice
            })
        }.run {
            if (serviceChoice != choice) customerSelectionRepository.save(apply { serviceChoice = choice }) else this
        }
    }

    fun <T> handleEvent(event: T, query: String, params: Map<String, String>) where T : Event, T : ReplyEvent {
        when (query) {
            SERVICE_SELECTED -> handleServiceSelectedEvent(event, params)
        }
    }

    val PARAMS = listOf(SERVICE_SELECTED, NAME_RECEIVED)


    val context: GeoApiContext = GeoApiContext.Builder().apiKey(apiKey).build()

    fun mapCustomerLocation(lat: Double, lon: Double): Location =
        //todo handle extreme case
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

    fun getInformationIfComplete(result: List<AddressComponent>): List<String>? {
        val provA = result.find { AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1 in it.types } ?: return null
        return if ("กรุงเทพมหานคร" in provA.longName) {
            val distA = result.find { AddressComponentType.SUBLOCALITY_LEVEL_1 in it.types } ?: return null
            val subdA = result.find { AddressComponentType.SUBLOCALITY_LEVEL_2 in it.types } ?: return null
            listOf(
                "กรุงเทพมหานคร",
                "แขวง" + distA.longName.replace("แขวง", "").trim(),
                "เขด" + subdA.longName.replace("เขด", "").trim()
            )
        } else {
            val distA = result.find { AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_2 in it.types } ?: return null
            val subdA = result.find { AddressComponentType.LOCALITY in it.types } ?: return null
            listOf(
                "จังหวัด" + provA.longName.replace("จังหวัด", "").trim(),
                "อำเภอ" + distA.longName.replace("อำเภอ", "").trim(),
                "ตำบล" + subdA.longName.replace("ตำบล", "").trim()
            )
        }
    }
}