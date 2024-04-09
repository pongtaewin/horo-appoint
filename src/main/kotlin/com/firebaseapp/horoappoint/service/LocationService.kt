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

@Deprecated("Use CustomerService")
@Service
class LocationService(
    @Value("\${gcp-maps.api-key}") val apiKey: String,
    val messageService: MessageService,
) {

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

}