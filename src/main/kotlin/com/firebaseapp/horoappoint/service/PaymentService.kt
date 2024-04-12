package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.entity.Appointment
import com.firebaseapp.horoappoint.repository.AppointmentRepository
import com.firebaseapp.horoappoint.repository.CustomerRepository
import com.firebaseapp.horoappoint.settings.ThaiFormatter
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.linecorp.bot.messaging.client.MessagingApiBlobClient
import com.linecorp.bot.messaging.model.CameraRollAction
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
    private val messageService: MessageService,
    private val messagingApiBlobClient: MessagingApiBlobClient,
    private val appointmentRepository: AppointmentRepository,
    private val customerRepository: CustomerRepository
) {


    fun getPaymentDueDateTime(appointment: Appointment) = ThaiFormatter.asZone(minOf<Instant>(
        appointment.selectionFinal!!.plus(1, ChronoUnit.DAYS),
        appointment.timeframe!!.startTime!!.minus(2, ChronoUnit.HOURS)
    ).also { if (it.isBefore(appointment.selectionFinal!!)) throw IllegalStateException("Impossible Booking") })


    fun <T> handlePaymentEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val appointment = appointmentRepository.findByEvent(event).get()
        if (appointment.selectionFinal == null) appointmentRepository.save(appointment.apply {
            selectionFinal = ThaiFormatter.now().toInstant()
        })

        val due = getPaymentDueDateTime(appointment)
        with(appointment) {
            messageService.replyMessage(
                event,
                getQRMessage(serviceChoice!!.getPriceRounded()),
                messageService.processTemplateAndMakeMessage(
                    "json/payment_info.txt", ModelMap().apply<ModelMap> {
                        set("service", serviceChoice!!.service!!.name)
                        set("choice", serviceChoice!!.name)
                        set("location", getLocationDescriptor())
                        set("date", timeframe!!.getCombinedDate())
                        set("time", timeframe!!.getCombinedTime())
                        set("customer", customer!!.fullName)
                        set("details", serviceChoice!!.getFullDescription())
                        set("subtotal", serviceChoice!!.getPriceRounded())
                        set("due_date", ThaiFormatter.format(due, "d MMM"))
                        set("due_time", ThaiFormatter.format(due, "H:mm"))
                    }, "กรุณาชำระเงิน",
                    QuickReplyItem(
                        URI("https://storage.googleapis.com/horo-appoint.appspot.com/bank.png"),
                        PostbackAction("ขอเลขที่บัญชี", "paymentID", "ขอเลขที่บัญชี", null, null, null)
                    )
                )
            )
        }
        customerRepository.save(customerRepository.findByEvent(event).get().apply { state = "slip" })
    }

    fun <T> handlePaymentIDEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val due = getPaymentDueDateTime(appointmentRepository.findByEvent(event).get())

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
        if (customerRepository.findByEvent(event).get().state != "slip") return

        val appointment = appointmentRepository.findByEvent(event).get()
        //if (appointment.isSlipFinal == true) todo lock final case

        appointment.slipAdded = ThaiFormatter.now().toInstant()

        val resource =
            "payment-slip-${String.format("%08d", appointment.id!!)}-${appointment.slipAdded!!.epochSecond}.jpg"
        storage.createFrom(
            BlobInfo.newBuilder("horo-appoint.appspot.com", resource).build(),
            getImageMessageContent((event.message as ImageMessageContent))
        )

        (appointment.slipImage)?.let { url ->
            storage.delete(
                BlobId.of(
                    "horo-appoint.appspot.com", url.toString()
                        .removePrefix("https://storage.googleapis.com/horo-appoint.appspot.com/")
                )
            )
        }

        appointment.slipImage = URL("https://storage.googleapis.com/horo-appoint.appspot.com/$resource")
        appointmentRepository.save(appointment)

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/slip_check.txt", ModelMap().apply {
                    set("uri", appointment.slipImage!!)
                    set("amount", appointment.serviceChoice!!.getPriceRounded())
                    set(
                        "uploaded",
                        ThaiFormatter.format(ThaiFormatter.asZone(appointment.slipAdded!!), "d MMM yyyy hh:mm:ss")
                    )
                }, "กรุณาตรวจสอบการชำระเงิน",
                QuickReplyItem(
                    URI("https://storage.googleapis.com/horo-appoint.appspot.com/receipt.png"),
                    CameraRollAction("อัปโหลดสลิปใหม่")
                )
            )
        )


    }

    fun <T> handleSlipUploadedEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {

        val appointment = appointmentRepository.findByEvent(event).get()
        appointment.slipFinal = ThaiFormatter.now().toInstant()
        appointmentRepository.save(appointment)
        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage("json/slip_uploaded.txt", ModelMap().apply {
                set("uri", appointment.slipImage!!)
                set("amount", appointment.serviceChoice!!.getPriceRounded())
                set(
                    "uploaded",
                    ThaiFormatter.format(ThaiFormatter.asZone(appointment.slipFinal!!), "d MMM yyyy hh:mm:ss")
                )
            }, "บันทึกหลักฐานการชำระเงินแล้ว")
        )
        customerRepository.save(customerRepository.findByEvent(event).get().apply { state = null })
    }

    fun getImageMessageContent(imageMessageContent: ImageMessageContent): InputStream =
        messagingApiBlobClient.getMessageContent(imageMessageContent.id).get().body.byteStream()


    companion object {
        fun getQRMessage(subtotal: String) = ImageMessage(
            URI("https://promptpay.io/0956394944/$subtotal.png"),
            URI("https://promptpay.io/0956394944/$subtotal.png")
        )
    }


}