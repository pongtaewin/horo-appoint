package com.firebaseapp.horoappoint.controller

import com.firebaseapp.horoappoint.HoroAppointApplication
import com.firebaseapp.horoappoint.entity.Customer
import com.firebaseapp.horoappoint.repository.CustomerRepository
import com.firebaseapp.horoappoint.service.CatalogService
import com.firebaseapp.horoappoint.service.DetailService
import com.firebaseapp.horoappoint.service.MessageService
import com.firebaseapp.horoappoint.service.PaymentService
import com.firebaseapp.horoappoint.service.SchedulingService
import com.linecorp.bot.messaging.client.MessagingApiClient
import com.linecorp.bot.messaging.model.ShowLoadingAnimationRequest
import com.linecorp.bot.spring.boot.handler.annotation.EventMapping
import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.FollowEvent
import com.linecorp.bot.webhook.model.ImageMessageContent
import com.linecorp.bot.webhook.model.LocationMessageContent
import com.linecorp.bot.webhook.model.MessageEvent
import com.linecorp.bot.webhook.model.PostbackEvent
import com.linecorp.bot.webhook.model.TextMessageContent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller

private const val LOADING_SECONDS = 30

@LineMessageHandler
@Controller
@Suppress("LongParameterList")
class LineBotController(
    private val messagingApiClient: MessagingApiClient,
    private val customerRepository: CustomerRepository,
    private val messageService: MessageService,
    private val catalogService: CatalogService,
    private val detailService: DetailService,
    private val schedulingService: SchedulingService,
    private val paymentService: PaymentService,
) {

    private val log = LoggerFactory.getLogger(HoroAppointApplication::class.java)

    // [method]?[param1]=[value1]&[param2]=[value2]&[param3]=[value3]...
    fun getParameters(postback: String): Pair<String, Map<String, String>> {
        val p = postback.indexOfFirst { it == '?' }

        if (p < 0) {
            require('=' !in postback) { "Found '=' in method '$postback'." }
        } else {
            require(p == postback.indexOfLast { it == '?' }) { "Found multiple '?' in '$postback'" }
        }

        val q = if (p < 0) postback else postback.take(p)
        require(q != "") { "No method in '$postback." }

        val m = if (p < 0) {
            mapOf()
        } else {
            postback.drop(p + 1).split("&")
                .associate { s -> s.split("=").let { (a, b) -> a to b } }
        }

        return q to m
    }

    fun showLoadingAnimation(userId: String) {
        messagingApiClient.showLoadingAnimation(ShowLoadingAnimationRequest(userId, LOADING_SECONDS))
    }

    @EventMapping
    fun handlePostbackEvent(event: PostbackEvent) {
        log.info("[LB] Postback Event: $event")
        showLoadingAnimation(event.source.userId())
        val (query, params) = getParameters(event.postback.data)
        when (query) {
            SERVICE_CATEGORY, SERVICE -> handleCatalogPostback(query, event, params)
            SERVICE_CHOICE, SELECT, LOCATION -> handleDetailPostback(query, event, params)
            DATE, TIME, SCHEDULE, NAME, BOOKING_CONFIRM -> handleSchedulingPostback(query, event, params)
            PAYMENT, PAYMENT_ID, SLIP_UPLOADED -> handlePaymentPostback(query, event, params)
            else -> return
        }
    }

    fun handleCatalogPostback(query: String, event: PostbackEvent, params: Map<String, String>) = when (query) {
        SERVICE_CATEGORY -> catalogService.handleServiceCategoryEvent(event, params)
        SERVICE -> catalogService.handleServiceEvent(event, params)
        else -> error("Illegal query: $query")
    }

    fun handleDetailPostback(query: String, event: PostbackEvent, params: Map<String, String>) = when (query) {
        SERVICE_CHOICE -> detailService.handleServiceChoiceEvent(event, params)
        SELECT -> detailService.handleSelectEvent(event, params)
        LOCATION -> detailService.handleLocationEvent(event, params)
        else -> error("Illegal query: $query")
    }

    fun handleSchedulingPostback(query: String, event: PostbackEvent, params: Map<String, String>) = when (query) {
        DATE -> schedulingService.handleDateEvent(event, params)
        TIME -> schedulingService.handleTimeEvent(event, params)
        SCHEDULE -> schedulingService.handleScheduleEvent(event, params)
        NAME -> schedulingService.handleNameEvent(event, params)
        BOOKING_CONFIRM -> schedulingService.handleConfirmEvent(event, params)
        else -> error("Illegal query: $query")
    }

    fun handlePaymentPostback(query: String, event: PostbackEvent, params: Map<String, String>) = when (query) {
        PAYMENT -> paymentService.handlePaymentEvent(event, params)
        PAYMENT_ID -> paymentService.handlePaymentIDEvent(event, params)
        SLIP_UPLOADED -> paymentService.handleSlipUploadedEvent(event, params)
        else -> error("Illegal query: $query")
    }

    @EventMapping
    fun handleMessageEvent(event: MessageEvent) {
        log.info("[LB] Message Event: $event")
        showLoadingAnimation(event.source.userId())
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
                detailService.handleLocationReceivedEvent(
                    event,
                    mapOf(
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
        showLoadingAnimation(event.source.userId())
        val uid = event.source.userId()
        customerRepository.findByLineUID(uid).ifPresentOrElse(
            { customer -> messageService.sendFollowMessage(event, customer) },
            {
                messagingApiClient.getProfile(uid).whenComplete { profile, throwable ->
                    throwable?.let { throw throwable }
                    messageService.sendFollowMessage(
                        event,
                        customerRepository.save(
                            Customer().apply {
                                lineUID = uid
                                displayName = profile.body.displayName
                                displayImage = profile.body.pictureUrl.toURL()
                            }
                        )
                    )
                }
            }
        )
    }

    @EventMapping
    fun handleDefaultMessageEvent(event: Event) {
        log.info("event: $event")
    }

    companion object {
        const val SERVICE_CATEGORY = "serviceCategory"
        const val SERVICE = "service"
        const val SERVICE_CHOICE = "serviceChoice"
        const val SELECT = "select"
        const val LOCATION = "location"
        const val DATE = "date"
        const val TIME = "time"
        const val SCHEDULE = "schedule"
        const val NAME = "name"
        const val BOOKING_CONFIRM = "bookingConfirm"
        const val PAYMENT = "payment"
        const val PAYMENT_ID = "paymentID"
        const val SLIP_UPLOADED = "slipUploaded"
    }
}
