package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.LineBotEventService
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.stereotype.Service

@Deprecated("Use LineBotController")
@Service
class PostbackHandlerService(
    val catalogService: CatalogService,
    val customerInfoService: CustomerInfoService,
    //val locationService: LocationService,
    val schedulingService: SchedulingService,
    //private val paymentInfoService: PaymentInfoService
) {


    fun <T> handlePostbackEvent(event: T, query: String, params: Map<String, String>) where T : Event, T : ReplyEvent {
        when (query) {
            in catalogService.PARAMS -> catalogService.handleEvent(event, query, params)
            in customerInfoService.PARAMS -> customerInfoService.handleEvent(event, query, params)
            //in locationService.PARAMS -> locationService.handleEvent(event, query, params)
            //in schedulingService.PARAMS -> schedulingService.handleEvent(event, query, params)
            //in paymentInfoService.PARAMS -> paymentInfoService.handleEvent(event, query, params)
            else -> return
        }
    }


}