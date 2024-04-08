package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.LineBotEventService
import com.firebaseapp.horoappoint.model.Location
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.model.AddressComponent
import com.google.maps.model.AddressComponentType
import com.google.maps.model.LatLng
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class LocationService(
    @Value("\${gcp-maps.api-key}") val apiKey: String,
    val messageService: MessageService,
) {

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
/*
    fun <T> handleCustomerLocationEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/customer_location.txt",
                ModelMap().apply { //todo Customer Location
                    set("uri", appointment.paymentImage!!)
                    set(
                        "uploaded",
                        ThaiFormatter.format(ThaiFormatter.asZone(appointment.uploaded!!), "d MM yyyy hh:mm:ss")
                    )
                },
                "บันทึกหลักฐานการชำระเงินแล้ว"
            )
        )
    }

    fun <T> handleCustomerLocationSelectedEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        //todo todo Check Type and cast to LocationEvent

        val location = ((event as MessageEvent).message as LocationMessageContent)
        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/slip_uploaded.txt",
                ModelMap().apply { //todo Customer Location
                    set("uri", appointment.paymentImage!!)
                    set(
                        "uploaded",
                        ThaiFormatter.format(ThaiFormatter.asZone(appointment.uploaded!!), "d MM yyyy hh:mm:ss")
                    )
                },
                "บันทึกหลักฐานการชำระเงินแล้ว"
            )
        )
    }

    fun <T> handleStaffLocationEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage("json/slip_uploaded.txt", ModelMap().apply {
                set("uri", appointment.paymentImage!!)
                set(
                    "uploaded",
                    ThaiFormatter.format(ThaiFormatter.asZone(appointment.uploaded!!), "d MM yyyy hh:mm:ss")
                )
            }, "บันทึกหลักฐานการชำระเงินแล้ว")
        )
    }

    fun <T> handleStaffLocationSelectedEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        //todo to Break
    }
*/
    fun <T> handleEvent(event: T, query: String, params: Map<String, String>) where T : Event, T : ReplyEvent {
        when (query) {
            //CUSTOMER_LOCATION -> handleCustomerLocationEvent(event, params)
            //STAFF_LOCATION -> handleStaffLocationEvent(event, params)
        }
    }

    companion object {
        const val CUSTOMER_LOCATION = "customerLocation"
        const val STAFF_LOCATION = "staffLocation"
        val PARAMS = listOf(CUSTOMER_LOCATION, STAFF_LOCATION)

    }
}