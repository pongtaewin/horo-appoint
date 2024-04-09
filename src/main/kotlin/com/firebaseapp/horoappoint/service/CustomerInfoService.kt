package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.LineBotEventService
import com.firebaseapp.horoappoint.model.Customer
import com.firebaseapp.horoappoint.model.CustomerSelection
import com.firebaseapp.horoappoint.model.Location
import com.firebaseapp.horoappoint.model.ServiceChoice
import com.firebaseapp.horoappoint.model.enums.SelectionState
import com.firebaseapp.horoappoint.model.enums.ServiceType
import com.firebaseapp.horoappoint.repository.CustomerRepository
import com.firebaseapp.horoappoint.repository.CustomerSelectionRepository
import com.firebaseapp.horoappoint.repository.ServiceChoiceRepository
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.model.AddressComponent
import com.google.maps.model.AddressComponentType
import com.google.maps.model.LatLng
import com.linecorp.bot.messaging.model.*
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

@Service
class CustomerInfoService(
    private val messageService: MessageService,
    private val serviceChoiceRepository: ServiceChoiceRepository,
    private val customerSelectionRepository: CustomerSelectionRepository,
    private val customerRepository: CustomerRepository,
    @Value("\${gcp-maps.api-key}") private val apiKey: String,
) {
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

    companion object {
        const val SERVICE_SELECTED = "serviceSelected"
        const val NAME_RECEIVED = "nameReceived"
    }

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