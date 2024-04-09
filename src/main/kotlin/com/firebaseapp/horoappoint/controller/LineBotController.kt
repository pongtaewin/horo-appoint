package com.firebaseapp.horoappoint.controller

import com.firebaseapp.horoappoint.HoroAppointApplication
import com.firebaseapp.horoappoint.model.*
import com.firebaseapp.horoappoint.model.enums.SelectionState
import com.firebaseapp.horoappoint.repository.CustomerRepository
import com.firebaseapp.horoappoint.repository.CustomerSelectionRepository
import com.firebaseapp.horoappoint.service.*
import com.firebaseapp.horoappoint.service.CatalogService.Companion.SELECT_SERVICE
import com.firebaseapp.horoappoint.service.CatalogService.Companion.SELECT_SERVICE_CATEGORY
import com.firebaseapp.horoappoint.service.CatalogService.Companion.SELECT_SERVICE_CHOICE
import com.linecorp.bot.messaging.client.MessagingApiClient
import com.linecorp.bot.spring.boot.handler.annotation.EventMapping
import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler
import com.linecorp.bot.webhook.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import kotlin.jvm.optionals.getOrNull


@LineMessageHandler
@Controller
class LineBotController(
    private val messagingApiClient: MessagingApiClient,
    //private val messagingApiBlobClient: MessagingApiBlobClient,
    private val customers: CustomerRepository,
    private val messageService: MessageService,
    private val catalogService: CatalogService,
    //private val postbackHandlerService: PostbackHandlerService,
    private val paymentMessageService: PaymentService,
    //private val appointmentRepository: AppointmentRepository,
    //private val schedulingService: SchedulingService,
    private val customerSelectionRepository: CustomerSelectionRepository,
    private val customerInfoService: CustomerInfoService,
    private val schedulingService: SchedulingService,
    private val paymentService: PaymentService
) {

    private val log = LoggerFactory.getLogger(HoroAppointApplication::class.java)


    //[method]?[param1]=[value1]&[param2]=[value2]&[param3]=[value3]...
    fun getParameters(postback: String): Pair<String, Map<String, String>> {
        val p = postback.indexOfFirst { it == '?' }

        if (p < 0) {
            if ('=' in postback)
                throw IllegalArgumentException("Found '=' in method '$postback'.")
        } else if (p != postback.indexOfLast { it == '?' })
            throw IllegalArgumentException("Found multiple '?' in '$postback'")

        val q = if (p < 0) postback else postback.take(p)
        if (q == "") throw IllegalArgumentException("No method in '$postback.")

        val m = if (p < 0) mapOf() else postback.drop(p + 1).split("&")
            .associate { s -> s.split("=").let { (a, b) -> a to b } }

        return q to m
    }

    companion object {
        val SERVICE_CATEGORY = SELECT_SERVICE_CATEGORY
        val SERVICE = SELECT_SERVICE
        val SERVICE_CHOICE = SELECT_SERVICE_CHOICE
        val LOCATION = "location"
        val DATE = "date"
        val TIME = "time"
        val NAME = "name"
        val BOOKING_CONFIRM = "bookingConfirm"
        val PAYMENT = "payment"
        val PAYMENT_ID = "paymentID"
        val SLIP_CONFIRM = "slipConfirm"
        val SLIP_UPLOADED = "slipUploaded"
    }

    @EventMapping
    fun handlePostbackEvent(event: PostbackEvent) {
        log.info("[LB] Postback Event: $event")
        val (query, params) = getParameters(event.postback.data)
        when (query) {
            SERVICE_CATEGORY -> catalogService.handleServiceCategoryEvent(event, params)
            SERVICE -> catalogService.handleServiceEvent(event, params)
            SERVICE_CHOICE -> catalogService.handleServiceChoiceEvent(event, params)
            LOCATION -> schedulingService.handleLocationEvent(event, params)
            DATE -> schedulingService.handleDateEvent(event, params)
            TIME -> println("todo") //schedulingService.handleTimeEvent(event, params) //scheduling, implemented
            NAME -> println("todo") //schedulingService.handleNameEvent(event, params) //not implemented, but easy
            BOOKING_CONFIRM -> println("todo") //schedulingService.handleConfirmEvent(event, params) // reuse the payment template
            PAYMENT -> paymentService.handlePaymentEvent(event, params)
            PAYMENT_ID -> paymentService.handlePaymentIDEvent(event, params)
            SLIP_CONFIRM -> println("todo") //paymentService.handleSlipConfirmEvent(event, params) // reuse the slipUploaded template
            SLIP_UPLOADED -> paymentService.handleSlipUploadedEvent(event, params)
            else -> return
        }
    }

    //แสดงกลุ่มบริการ
    //แสดงบริการในกลุ่ม
    //เลือกบริการ
    //เลือก

    @EventMapping
    fun handleMessageEvent(event: MessageEvent) {
        log.info("[LB] Message Event: $event")
        when (val message = event.message) {
            is ImageMessageContent -> {
                paymentService.handleUploadSlipEvent(event, mapOf())
            }

            is TextMessageContent -> {
                /*val selection = customerSelectionRepository.findByCustomer_LineUID(event.source.userId()).getOrNull()
                if (selection?.getSelectionState() == SelectionState.CUSTOMER_NAME_REQUIRED) {
                    customerInfoService.handleEvent(event, CustomerInfoService.NAME_RECEIVED, mapOf())
                } else when (message.text) {

                 */
                    /*"QR" -> paymentMessageService.sendPaymentInfoMessages(
                        event,
                        Appointment().apply {
                            customer = Customer().apply {
                                fullName = "สมาทาน ธรรมศีล"
                            }
                            serviceChoice = ServiceChoice().apply {
                                name = "ดูดวงระยะสั้น 15 นาที"
                                serviceType = ServiceType.MEETUP
                                price = 240.0
                                durationMinutes = 15
                                paddingAfterMinutes = 15
                                paddingBeforeMinutes = 0
                            }
                            timeframe = Timeframe().apply {
                                startTime = Instant.parse("2024-04-09T09:00:00.00Z")
                                endTime = Instant.parse("2024-04-09T09:15:00.00Z")
                            }
                            location = Location().apply {
                                this.province = "จังหวัดกรุงเทพมหานคร"
                                this.district = "เขตวังทองหลาง"
                                this.subdistrict = "แขวงคลองเจ้าคุณสิงห์"
                            }
                            created = Instant.parse("2024-04-05T12:03:00.00Z")
                        })

                     */
                    /*
                    "Schedule" -> schedulingService.sendSchedulingMessage(
                        event,
                        CustomerSelection().apply {
                            service = Service().apply {
                                name = "แพ็กเกจดูดวงเร่งด่วน"
                                description = "สื่อจิตสำรวจกรรม ปรับพื้นดวง\\nพร้อมให้คำแนะนำแนวทางแก้ปัญหา\\n" +
                                        "โดยการพิมพ์ตอบคำถามใน\\nช่องการสนทนาทางแซทไลน์"
                                //serviceType = ServiceType.ONLINE_CHAT
                                //durationType = DurationType.TIMED
                                //durationMinutes = 15
                            }
                            //price = 240.0
                        },
                        LocalDate.of(2024, 3, 25)
                    )
                    */
                //}
            }

        }
    }

    @EventMapping
    fun handleFollowEvent(event: FollowEvent) {
        log.info("[LB] Follow Event: $event")

        val uid = event.source.userId()
        customers.findByLineUID(uid).ifPresentOrElse(
            { customer -> messageService.sendFollowMessage(event, customer) },
            {
                messagingApiClient.getProfile(uid).whenComplete { profile, throwable ->
                    throwable?.let { throw throwable }
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

