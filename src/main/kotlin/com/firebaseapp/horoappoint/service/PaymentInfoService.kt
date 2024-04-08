package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.LineBotEventService
import com.firebaseapp.horoappoint.model.Appointment
import com.firebaseapp.horoappoint.repository.AppointmentRepository
import com.firebaseapp.horoappoint.settings.ThaiFormatter

import com.linecorp.bot.messaging.model.ImageMessage
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class PaymentInfoService(
    val messageService: MessageService,
    private val appointmentRepository: AppointmentRepository
) {


    fun getPaymentDueDateTime(appointment: Appointment) = ThaiFormatter.asZone(minOf<Instant>(
        appointment.created!!.plus(1, ChronoUnit.DAYS),
        appointment.timeframe!!.startTime!!.minus(2, ChronoUnit.HOURS)
    ).also { if (it.isBefore(appointment.created!!)) throw IllegalStateException("Impossible Booking") })

    fun sendPaymentInfoMessages(event: ReplyEvent, appointment: Appointment) {
        val due = getPaymentDueDateTime(appointment)
        with(appointment) {
            messageService.replyMessage(
                event,
                getQRMessage(serviceChoice!!.getPriceRounded()),
                messageService.processTemplateAndMakeMessage("json/payment_info.txt", ModelMap().apply {
                    set("service", serviceChoice!!.name)
                    set("location", getLocationDescriptor())
                    set("date", timeframe!!.getCombinedDate())
                    set("time", timeframe!!.getCombinedTime())
                    set("customer", customer!!.fullName)
                    set("details", serviceChoice!!.getFullDescription())
                    set("subtotal", serviceChoice!!.getPriceRounded())
                    set("due_date", ThaiFormatter.format(due, "d MMM"))
                    set("due_time", ThaiFormatter.format(due, "H:mm"))
                }, "กรุณาชำระเงิน")
            )
        }
    }


    fun <T> handlePaymentInfoEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val appointment = appointmentRepository.findById(params["id"]!!.toLong()).get()
        sendPaymentInfoMessages(event, appointment)
    }

    fun <T> handlePaymentIDEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val due = getPaymentDueDateTime(appointmentRepository.findById(params["id"]!!.toLong()).get())

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage("json/payment_id.txt", ModelMap().apply {
                set("due_date", ThaiFormatter.format(due, "d MMM"))
                set("due_time", ThaiFormatter.format(due, "H:mm"))
            }, "กรุณาชำระเงิน")
        )

    }


    fun <T> handleEvent(event: T, query: String, params: Map<String, String>) where T : Event, T : ReplyEvent {
        when (query) {
            PAYMENT_INFO -> handlePaymentInfoEvent(event, params)
            PAYMENT_ID -> handlePaymentIDEvent(event, params)
        }
    }

    companion object {
        const val PAYMENT_INFO = "paymentInfo"
        const val PAYMENT_ID = "paymentId"
        val PARAMS = listOf(PAYMENT_INFO, PAYMENT_ID)

        fun getQRMessage(subtotal: String) = ImageMessage(
            URI("https://promptpay.io/0956394944/$subtotal.png"),
            URI("https://promptpay.io/0956394944/$subtotal.png")
        )
    }


}