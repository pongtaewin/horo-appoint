package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.model.Appointment
import com.firebaseapp.horoappoint.settings.ThaiDateTimeFormatters

import com.linecorp.bot.messaging.model.ImageMessage
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import java.net.URI
import java.time.temporal.ChronoUnit

@Service
class PaymentMessageService(
    val messageService: MessageService
) {


    fun sendPaymentInfoMessages(event: ReplyEvent, appointment: Appointment) {
        sendPaymentInfoMessages(event, ModelMap().apply {
            addAttribute("service", appointment.service!!.name)
            appointment.location?.getName().let{addAttribute("location", it)}
            addAttribute("date", appointment.timeframe!!.getCombinedDate())
            addAttribute("time", appointment.timeframe!!.getCombinedTime())
            addAttribute("customer", appointment.customer!!.getFullName())
            //addAttribute("details") todo setup details page
            addAttribute("subtotal", "240.00") //todo remove hardcode
            val due = minOf(
                appointment.created!!.plus(1, ChronoUnit.DAYS),
                appointment.timeframe!!.startTime!!.minus(2, ChronoUnit.HOURS)
            )
            if (due.isBefore(appointment.created!!)) throw IllegalArgumentException("Impossible Booking")
            addAttribute("due_date", ThaiDateTimeFormatters.withZone(due).format(ThaiDateTimeFormatters.of("d MMM")))
            addAttribute("due_time", ThaiDateTimeFormatters.withZone(due).format(ThaiDateTimeFormatters.of("H:mm")))
        })
    }

    fun sendPaymentInfoMessages(event: ReplyEvent, modelMap: ModelMap) {
        messageService.replyMessage(
            event,
            ImageMessage(
                URI("https://promptpay.io/0956394944/${modelMap["subtotal"]}.png"),
                URI("https://promptpay.io/0956394944/${modelMap["subtotal"]}.png")
            ),
            messageService.processTemplateAndMakeMessage("json/payment_info.txt", modelMap, "กรุณาชำระเงิน")
        )
    }
}