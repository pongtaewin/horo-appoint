package com.firebaseapp.horoappoint.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.firebaseapp.horoappoint.HoroAppointApplication.Companion.getImg
import com.firebaseapp.horoappoint.entity.Customer
import com.firebaseapp.horoappoint.repository.CustomerRepository
import com.linecorp.bot.messaging.client.MessagingApiClient
import com.linecorp.bot.messaging.model.Action
import com.linecorp.bot.messaging.model.FlexMessage
import com.linecorp.bot.messaging.model.Message
import com.linecorp.bot.messaging.model.PostbackAction
import com.linecorp.bot.messaging.model.PushMessageRequest
import com.linecorp.bot.messaging.model.QuickReply
import com.linecorp.bot.messaging.model.QuickReplyItem
import com.linecorp.bot.messaging.model.ReplyMessageRequest
import com.linecorp.bot.messaging.model.TextMessage
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.FollowEvent
import com.linecorp.bot.webhook.model.ReplyEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.io.StringWriter
import kotlin.jvm.optionals.getOrNull

@Service
class MessageService(
    private val messagingApiClient: MessagingApiClient,
    private val templateEngine: SpringTemplateEngine
) {

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    val objectMapper: ObjectMapper =
        ObjectMapper().run { configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) }

    fun sendFollowMessage(event: FollowEvent, customer: Customer) {
        replyMessage(
            event,
            processTemplateAndMakeMessage(
                "json/greetings.txt",
                ModelMap().apply {
                    this["name"] = customer.displayName ?: "ลูกค้า"
                    this["img"] = customer.displayImage
                    this["uid"] = customer.lineUID
                },
                "ยินดีต้อนรับ"
            )
        )
    }

    fun <T> replyMessage(event: T, vararg messages: Message?) where T : Event, T : ReplyEvent {
        val customer = customerRepository.findByEvent(event).getOrNull()
        when (customer?.state) {
            "name" -> {
                customerRepository.save(customer.apply { state = null })
                return reply(event, listOf(TextMessage("[ระบบปิดรับคำตอบ]")) + messages.filterNotNull())
            }

            "location" -> {
                customerRepository.save(customer.apply { state = null })
            }
        }
        reply(event, messages.filterNotNull())
    }

    fun pushMessage(customer: Customer, vararg messages: Message) {
        messagingApiClient.pushMessage(null, PushMessageRequest(customer.lineUID!!, listOf(*messages), false, null))
    }

    private fun reply(event: ReplyEvent, messages: List<Message>) {
        messagingApiClient.replyMessage(ReplyMessageRequest(event.replyToken(), messages, false))
    }

    val log: Logger = LoggerFactory.getLogger(MessageService::class.java)

    fun processTemplateAndMakeMessage(
        template: String,
        modelMap: ModelMap,
        altText: String,
        vararg quickReplyItems: QuickReplyItem?
    ): FlexMessage = objectMapper.readValue(
        buildString {
            append("{\"type\":\"flex\",")
            append("\"altText\":\"$altText\",")
            if (quickReplyItems.isNotEmpty()) {
                append(
                    "\"quickReply\":${
                        objectMapper.writeValueAsString(QuickReply(listOfNotNull(*quickReplyItems)))
                    },"
                )
            }
            append("\"contents\":${processJSONToString(modelMap, template)}}")
        }.also { log.debug(it) },
        FlexMessage::class.java
    )

    fun processJSONToString(modelMap: ModelMap, template: String) = StringWriter().apply {
        templateEngine.process(template, Context().apply { setVariables(modelMap) }, this)
    }.toString()

    fun quickReplyOf(
        icon: String,
        label: String,
        data: String,
        vararg params: Pair<String, String>
    ) = quickReplyOf(
        icon,
        PostbackAction(
            label,
            "$data${(if (params.isNotEmpty()) "?" else "") + params.joinToString("&") { (k, v) -> "$k=$v" }}",
            label,
            null,
            null,
            null
        )
    )

    fun quickReplyOf(icon: String, action: Action) =
        QuickReplyItem(getImg(icon).toURI(), action)
}
