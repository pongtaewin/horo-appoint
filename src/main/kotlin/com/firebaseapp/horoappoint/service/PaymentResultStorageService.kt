package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.LineBotEventService
import com.firebaseapp.horoappoint.repository.AppointmentRepository
import com.firebaseapp.horoappoint.settings.ThaiFormatter
import com.google.cloud.storage.*
import com.linecorp.bot.messaging.client.MessagingApiBlobClient
import com.linecorp.bot.webhook.model.*
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import java.io.InputStream
import java.net.URL

@Service
class PaymentResultStorageService(
    private val appointmentRepository: AppointmentRepository,
    private val messagingApiBlobClient: MessagingApiBlobClient,
    private val messageService: MessageService,

    )  {

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
        const val SLIP_UPLOADED = "slipUploaded"
    }

    val PARAMS: List<String> = listOf(SLIP_UPLOADED)

    fun <T> handleEvent(event: T, query: String, params: Map<String, String>) where T : Event, T : ReplyEvent {
        when (query) {
            SLIP_UPLOADED -> handleSlipUploadedEvent(event, params)
        }
    }

}