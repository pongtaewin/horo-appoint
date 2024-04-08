package com.firebaseapp.horoappoint.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.firebaseapp.horoappoint.model.Customer
import com.linecorp.bot.messaging.client.MessagingApiClient
import com.linecorp.bot.messaging.model.*
import com.linecorp.bot.webhook.model.FollowEvent
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.io.StringWriter

@Service
class MessageService(
    val messagingApiClient: MessagingApiClient,
    val templateEngine: SpringTemplateEngine
) {
    @Autowired
    val objectMapper: ObjectMapper =
        ObjectMapper().run { configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) }

    fun sendFollowMessage(event: FollowEvent, customer: Customer) {
        replyMessage(event, processTemplateAndMakeMessage("json/greetings.txt", ModelMap().apply {
            addAttribute("name", customer.displayName ?: "ลูกค้า")
            addAttribute("img", customer.displayImage)
            addAttribute("uid", customer.lineUID)
        }, "ยินดีต้อนรับ"))
    }

    fun replyMessage(event: ReplyEvent, vararg messages: Message) {
        messagingApiClient.replyMessage(ReplyMessageRequest(event.replyToken(), listOf(*messages), false))
    }

    fun processTemplateAndMakeMessage(
        template: String, modelMap: ModelMap, altText: String, vararg quickReplyItems: QuickReplyItem
    ): FlexMessage = objectMapper.readValue(
        buildString {
            append("{\"type\":\"flex\",")
            append("\"altText\":\"$altText\",")
            if (quickReplyItems.isNotEmpty())
                append("\"quickReply\":${objectMapper.writeValueAsString(QuickReply(listOf(*quickReplyItems)))},")
            append("\"contents\":${processJSONToString(modelMap, template)}}")
        },
        FlexMessage::class.java
    )


    fun processJSONToString(modelMap: ModelMap, template: String) = StringWriter().apply {
        templateEngine.process(template, Context().apply { setVariables(modelMap) }, this)
    }.toString()

}
