package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.HoroAppointApplication.Companion.BUCKET_LINK
import com.firebaseapp.horoappoint.HoroAppointApplication.Companion.PROJECT_ID
import com.firebaseapp.horoappoint.HoroAppointApplication.Companion.PROJECT_LINK
import com.firebaseapp.horoappoint.HoroAppointApplication.Companion.getImg
import com.firebaseapp.horoappoint.ThaiFormatter
import com.firebaseapp.horoappoint.entity.Appointment
import com.firebaseapp.horoappoint.repository.AppointmentRepository
import com.firebaseapp.horoappoint.repository.CustomerRepository
import com.firebaseapp.horoappoint.service.SchedulingService.Companion.addAppointmentDetails
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.linecorp.bot.messaging.client.MessagingApiBlobClient
import com.linecorp.bot.messaging.model.CameraRollAction
import com.linecorp.bot.messaging.model.ImageMessage
import com.linecorp.bot.messaging.model.PostbackAction
import com.linecorp.bot.messaging.model.QuickReplyItem
import com.linecorp.bot.messaging.model.TextMessage
import com.linecorp.bot.webhook.model.ImageMessageContent
import com.linecorp.bot.webhook.model.MessageEvent
import com.linecorp.bot.webhook.model.PostbackEvent
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import java.io.InputStream
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class PaymentService(
    private val messageService: MessageService,
    private val messagingApiBlobClient: MessagingApiBlobClient,
    private val appointmentRepository: AppointmentRepository,
    private val customerRepository: CustomerRepository
) {

    fun getPaymentDueDateTime(appointment: Appointment) = ThaiFormatter.asZone(
        minOf<Instant>(
            appointment.selectionFinal!!.plus(1, ChronoUnit.DAYS),
            appointment.timeframe!!.startTime!!.minus(2, ChronoUnit.HOURS)
        ).also { require(it.isAfter(appointment.selectionFinal!!)) { "Impossible Booking" } }
    )

    @Suppress("UnusedParameter")
    fun handlePaymentEvent(event: PostbackEvent, params: Map<String, String>) {
        val appointment = appointmentRepository.findByEvent(event).get()
        if (appointment.selectionFinal == null) {
            appointmentRepository.save(
                appointment.apply {
                    selectionFinal = ThaiFormatter.now().toInstant()
                }
            )
        }

        val due = getPaymentDueDateTime(appointment)
        with(appointment) {
            messageService.replyMessage(
                event,
                getQRMessage(serviceChoice!!.getPriceRounded()),
                messageService.processTemplateAndMakeMessage(
                    "json/payment_info.txt",
                    ModelMap().apply {
                        set("service", serviceChoice!!.service!!.name)
                        set("choice", serviceChoice!!.name)
                        set("location", getLocationDescriptor())
                        set("date", timeframe!!.getCombinedDate())
                        set("time", timeframe!!.getCombinedTime())
                        set("customer", customer!!.fullName)
                        set("details", serviceChoice!!.getFullDescription())
                        set("subtotal", serviceChoice!!.getPriceRounded())
                        set("due_date", ThaiFormatter.format(due, "d MMM"))
                        set("due_time", ThaiFormatter.format(due, "H:mm") + " น.")
                    },
                    "กรุณาชำระเงิน",
                    QuickReplyItem(
                        getImg("bank.png").toURI(),
                        PostbackAction("แสดงเลขที่บัญชี", "paymentID", "แสดงเลขที่บัญชี", null, null, null)
                    )
                )
            )
        }
        customerRepository.save(customerRepository.findByEvent(event).get().apply { state = "slip" })
    }

    @Suppress("UnusedParameter")
    fun handlePaymentIDEvent(event: PostbackEvent, params: Map<String, String>) {
        val due = getPaymentDueDateTime(appointmentRepository.findByEvent(event).get())

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/payment_id.txt",
                ModelMap().apply {
                    set("due_date", ThaiFormatter.format(due, "d MMM"))
                    set("due_time", ThaiFormatter.format(due, "H:mm") + " น.")
                },
                "กรุณาชำระเงิน"
            )
        )
    }

    val storage: Storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().service

    @Suppress("UnusedParameter")
    fun handleUploadSlipEvent(event: MessageEvent, params: Map<String, String>) {
        if (customerRepository.findByEvent(event).get().state != "slip") return

        val appointment = appointmentRepository.findByEvent(event).get()

        appointment.slipAdded = ThaiFormatter.now().toInstant()

        val resource = "payment-slip-${String.format(Locale.ENGLISH, "%08d", appointment.id!!)}" +
            "-${appointment.slipAdded!!.epochSecond}.jpg"
        storage.createFrom(
            BlobInfo.newBuilder(PROJECT_LINK, resource).build(),
            getImageMessageContent((event.message as ImageMessageContent))
        )

        (appointment.slipImage)?.let { url ->
            storage.delete(BlobId.of(PROJECT_LINK, url.toString().removePrefix(BUCKET_LINK)))
        }

        appointment.slipImage = getImg(resource)
        appointmentRepository.save(appointment)

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/slip_check.txt",
                ModelMap().apply {
                    set("uri", appointment.slipImage!!)
                    set("amount", appointment.serviceChoice!!.getPriceRounded())
                    set(
                        "uploaded",
                        ThaiFormatter.format(ThaiFormatter.asZone(appointment.slipAdded!!), "d MMM yyyy HH:mm:ss")
                    )
                },
                "กรุณาตรวจสอบการชำระเงิน",
                QuickReplyItem(
                    getImg("receipt.png").toURI(),
                    CameraRollAction("อัปโหลดสลิปใหม่")
                )
            )
        )
    }

    @Suppress("UnusedParameter")
    fun handleSlipUploadedEvent(event: PostbackEvent, params: Map<String, String>) {
        val appointment = appointmentRepository.findByEvent(event).get()
        appointment.slipFinal = ThaiFormatter.now().toInstant()
        appointmentRepository.save(appointment)
        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/slip_uploaded.txt",
                ModelMap().apply {
                    set("uri", appointment.slipImage!!)
                    set("amount", appointment.serviceChoice!!.getPriceRounded())
                    set(
                        "uploaded",
                        ThaiFormatter.format(ThaiFormatter.asZone(appointment.slipFinal!!), "d MMM yyyy HH:mm:ss")
                    )
                },
                "บันทึกหลักฐานการชำระเงินแล้ว"
            )
        )
        customerRepository.save(customerRepository.findByEvent(event).get().apply { state = null })
    }

    fun handleSuccessEvent(appointment: Appointment) {
        messageService.pushMessage(
            appointment.customer!!,
            TextMessage(
                "อาจารย์ตรวจสอบการชำระเงิน\n" +
                    "และยืนยันการนัดหมายเรียบร้อยแล้ว\n" +
                    "กรุณารอรับบริการตามเวลาที่ท่านนัดหมายครับ\n" +
                    "ขอบคุณครับ"
            ),
            messageService.processTemplateAndMakeMessage(
                "json/success.txt",
                ModelMap().addAppointmentDetails(appointment),
                "ยืนยันการนัดหมายแล้ว"
            )
        )
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
