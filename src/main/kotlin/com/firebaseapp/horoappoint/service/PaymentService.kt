package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.model.Appointment
import com.firebaseapp.horoappoint.repository.AppointmentRepository
import com.firebaseapp.horoappoint.settings.ThaiFormatter
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.linecorp.bot.messaging.client.MessagingApiBlobClient

import com.linecorp.bot.messaging.model.ImageMessage
import com.linecorp.bot.messaging.model.PostbackAction
import com.linecorp.bot.messaging.model.QuickReplyItem
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.ImageMessageContent
import com.linecorp.bot.webhook.model.MessageEvent
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class PaymentService(
    val messageService: MessageService,
    private val appointmentRepository: AppointmentRepository,
    private val messagingApiBlobClient: MessagingApiBlobClient
) {


    fun getPaymentDueDateTime(appointment: Appointment) = ThaiFormatter.asZone(minOf<Instant>(
        appointment.created!!.plus(1, ChronoUnit.DAYS),
        appointment.timeframe!!.startTime!!.minus(2, ChronoUnit.HOURS)
    ).also { if (it.isBefore(appointment.created!!)) throw IllegalStateException("Impossible Booking") })


    fun <T> handlePaymentEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val appointment = appointmentRepository.findById(params["id"]!!.toLong()).get()
        val due = getPaymentDueDateTime(appointment)
        with(appointment) {
            messageService.replyMessage(
                event,
                getQRMessage(serviceChoice!!.getPriceRounded()),
                messageService.processTemplateAndMakeMessage(
                    "json/payment_info.txt", ModelMap().apply<ModelMap> {
                        set("service", serviceChoice!!.name)
                        set("location", getLocationDescriptor())
                        set("date", timeframe!!.getCombinedDate())
                        set("time", timeframe!!.getCombinedTime())
                        set("customer", customer!!.fullName)
                        set("details", serviceChoice!!.getFullDescription())
                        set("subtotal", serviceChoice!!.getPriceRounded())
                        set("due_date", ThaiFormatter.format(due, "d MMM"))
                        set("due_time", ThaiFormatter.format(due, "H:mm"))
                    }, "กรุณาชำระเงิน",
                    QuickReplyItem(PostbackAction("ขอเลขที่บัญชี", "paymentId", "ขอเลขที่บัญชี", null, null, null))
                )
            )
        }
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


    val storage: Storage = StorageOptions.newBuilder().setProjectId("horo-appoint").build().service

    fun handleUploadSlipEvent(event: MessageEvent, params: Map<String, String>) {
        val appointment = appointmentRepository.findByEvent(event).also {
            if (it.isEmpty) println("TODO()")
        }.get()
        //if (appointment.isSlipFinal == true) TODO()

        appointment.uploaded = ThaiFormatter.now().toInstant()
        val name = "payment-slip-${String.format("%08d", appointment.id!!)}-${appointment.uploaded!!.epochSecond}"
        storage.createFrom(
            BlobInfo.newBuilder("horo-appoint.appspot.com", name).build(),
            getImageMessageContent((event.message as ImageMessageContent))
        )

        //todo remove image if overwritten
        appointment.paymentImage = URL("https://storage.googleapis.com/horo-appoint.appspot.com/$name.jpg")
        appointmentRepository.save(appointment)
        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage("json/slip_check.txt", ModelMap().apply {
                set("uri", appointment.paymentImage!!)
                set("amount", appointment.serviceChoice!!.getPriceRounded())
                set(
                    "uploaded",
                    ThaiFormatter.format(ThaiFormatter.asZone(appointment.uploaded!!), "d MM yyyy hh:mm:ss")
                )
            }, "กรุณาตรวจสอบการชำระเงิน")
        )
    }


    fun <T> handleSlipUploadedEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val appointment = appointmentRepository.findByEvent(event).get()
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

    fun getImageMessageContent(imageMessageContent: ImageMessageContent): InputStream =
        messagingApiBlobClient.getMessageContent(imageMessageContent.id).get().body.byteStream()


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