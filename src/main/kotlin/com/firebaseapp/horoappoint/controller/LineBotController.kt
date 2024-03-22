package com.firebaseapp.horoappoint.controller

import com.firebaseapp.horoappoint.HoroAppointApplication
import com.firebaseapp.horoappoint.model.*
import com.firebaseapp.horoappoint.model.enums.DurationType
import com.firebaseapp.horoappoint.model.enums.ServiceType
import com.firebaseapp.horoappoint.repository.CustomerRepository
import com.firebaseapp.horoappoint.service.MessageService
import com.firebaseapp.horoappoint.service.PaymentInfoService
import com.firebaseapp.horoappoint.service.SchedulingService
import com.linecorp.bot.messaging.client.MessagingApiClient
import com.linecorp.bot.spring.boot.handler.annotation.EventMapping
import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.FollowEvent
import com.linecorp.bot.webhook.model.MessageEvent
import com.linecorp.bot.webhook.model.TextMessageContent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import java.time.Instant
import java.time.LocalDate


@LineMessageHandler
@Controller
class LineBotController(
    private val messagingApiClient: MessagingApiClient,
    //private val messagingApiBlobClient: MessagingApiBlobClient,
    private val customers: CustomerRepository,
    private val messageService: MessageService,
    private val paymentMessageService: PaymentInfoService,
    private val schedulingService: SchedulingService
) {

    private val log = LoggerFactory.getLogger(HoroAppointApplication::class.java)

    @EventMapping
    fun handleMessageEvent(event: MessageEvent) {
        log.info("[LB] Msg Event: $event")
        when (val message = event.message) {

            is TextMessageContent -> when (message.text) {
                "QR" -> paymentMessageService.sendPaymentInfoMessages(event, Appointment().apply {
                    customer = Customer().apply {
                        firstName = "สมาทาน"
                        lastName = "ธรรมศีล"
                    }
                    service = Service().apply { name = "ดูดวงระยะสั้น 15 นาที" }
                    timeframe = Timeframe().apply {
                        startTime = Instant.parse("2024-03-20T09:00:00.00Z")
                        endTime = Instant.parse("2024-03-20T09:15:00.00Z")
                    }
                    location = CustomerLocation().apply {
                        this.province = "จังหวัดกรุงเทพมหานคร"
                        this.district = "เขตวังทองหลาง"
                        this.subdistrict = "แขวงคลองเจ้าคุณสิงห์"
                    }
                    created = Instant.parse("2024-03-15T12:03:00.00Z")
                })

                "Schedule" -> schedulingService.sendSchedulingMessage(event, CustomerSelection().apply {
                    service = Service().apply {
                        name = "แพ็กเกจดูดวงเร่งด่วน"
                        description = "สื่อจิตสำรวจกรรม ปรับพื้นดวง\\nพร้อมให้คำแนะนำแนวทางแก้ปัญหา\\n" +
                                "โดยการพิมพ์ตอบคำถามใน\\nช่องการสนทนาทางแซทไลน์"
                        serviceType = ServiceType.ONLINE_CHAT
                        durationType = DurationType.TIMED
                        durationMinutes = 15
                    }
                    price = 240.0
                }, LocalDate.of(2024, 3, 25))
            }


        }
    }

    @EventMapping
    fun handleFollowEvent(event: FollowEvent) {
        log.info("[LB] Follow: $event")

        val uid = event.source.userId()

        customers.findByLineUID(uid).ifPresentOrElse(
            { customer -> messageService.sendFollowMessage(event, customer) },
            {
                messagingApiClient.getProfile(uid).whenComplete { profile, throwable ->
                    if (throwable != null) throw throwable
                    messageService.sendFollowMessage(event, customers.save(Customer().apply {
                        lineUID = uid
                        displayName = profile.body.displayName
                        displayImage = profile.body.pictureUrl.toURL()
                    }))
                }
            }
        )
    }

    @EventMapping
    fun handleDefaultMessageEvent(event: Event) {
        log.info("event: $event")
    }

}


/*is ImageMessageContent -> {
                // generate file name
                val ref = "test"
                // save image to Storage Bucket
                val x = messagingApiBlobClient.getMessageContent(message.id).thenAccept {
                    val t = it.body.byteStream()
                    st.writer(BlobInfo.newBuilder(bk.asBucketInfo(), ref).build())
                        .use { writer ->
                            val bs= Channels.newOutputStream(writer).apply{ByteStreams.copy(t, this)}
                            t.transferTo(bs)
                        }
                }
            }*/
