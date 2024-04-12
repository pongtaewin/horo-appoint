package com.firebaseapp.horoappoint.controller

import com.firebaseapp.horoappoint.HoroAppointApplication
import com.firebaseapp.horoappoint.entity.Customer
import com.firebaseapp.horoappoint.repository.CustomerRepository
import com.firebaseapp.horoappoint.service.CatalogService
import com.firebaseapp.horoappoint.service.MessageService
import com.firebaseapp.horoappoint.service.PaymentService
import com.firebaseapp.horoappoint.service.SchedulingService
import com.linecorp.bot.messaging.client.MessagingApiClient
import com.linecorp.bot.spring.boot.handler.annotation.EventMapping
import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler
import com.linecorp.bot.webhook.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller


@LineMessageHandler
@Controller
class LineBotController(
    private val messagingApiClient: MessagingApiClient,
    private val customerRepository: CustomerRepository,
    private val messageService: MessageService,
    private val catalogService: CatalogService,
    private val schedulingService: SchedulingService,
    private val paymentService: PaymentService,
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
        val SERVICE_CATEGORY = "serviceCategory"
        val SERVICE = "service"
        val SERVICE_CHOICE = "serviceChoice"
        val SELECT = "select"
        val LOCATION = "location"
        val DATE = "date"
        val TIME = "time"
        val SCHEDULE = "schedule"
        val NAME = "name"
        val BOOKING_CONFIRM = "bookingConfirm"
        val PAYMENT = "payment"
        val PAYMENT_ID = "paymentID"
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
            SELECT -> schedulingService.handleSelectEvent(event,params)
            LOCATION -> schedulingService.handleLocationEvent(event, params)
            DATE -> schedulingService.handleDateEvent(event, params)
            TIME -> schedulingService.handleTimeEvent(event, params)
            SCHEDULE -> schedulingService.handleScheduleEvent(event, params)
            NAME -> schedulingService.handleNameEvent(event, params)
            BOOKING_CONFIRM -> schedulingService.handleConfirmEvent(event, params)
            PAYMENT -> paymentService.handlePaymentEvent(event, params)
            PAYMENT_ID -> paymentService.handlePaymentIDEvent(event, params)
            SLIP_UPLOADED -> paymentService.handleSlipUploadedEvent(event, params)
            else -> return
        }
    }

    @EventMapping
    fun handleMessageEvent(event: MessageEvent) {
        log.info("[LB] Message Event: $event")
        when (val message = event.message) {
            is TextMessageContent -> {
                val customer = customerRepository.findByEvent(event).get()
                when (customer.state) {
                    "name" -> {
                        customerRepository.save(customer.apply { fullName = message.text })
                        schedulingService.handleNameReceivedEvent(event, mapOf())
                    }
                }
            }

            is ImageMessageContent -> {
                paymentService.handleUploadSlipEvent(event, mapOf())
            }

            is LocationMessageContent -> {
                schedulingService.handleLocationReceivedEvent(
                    event, mapOf(
                        "lat" to message.latitude.toString(),
                        "lon" to message.longitude.toString(),
                        "title" to message.title,
                        "desc" to message.address
                    )
                )
            }
        }
    }

    @EventMapping
    fun handleFollowEvent(event: FollowEvent) {
        log.info("[LB] Follow Event: $event")

        val uid = event.source.userId()
        customerRepository.findByLineUID(uid).ifPresentOrElse(
            { customer -> messageService.sendFollowMessage(event, customer) },
            {
                messagingApiClient.getProfile(uid).whenComplete { profile, throwable ->
                    throwable?.let { throw throwable }
                    messageService.sendFollowMessage(event, customerRepository.save(Customer().apply {
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

