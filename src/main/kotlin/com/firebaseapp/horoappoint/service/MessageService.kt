package com.firebaseapp.horoappoint.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.firebaseapp.horoappoint.model.Customer
import com.linecorp.bot.messaging.client.MessagingApiClient
import com.linecorp.bot.messaging.model.FlexMessage
import com.linecorp.bot.messaging.model.Message
import com.linecorp.bot.messaging.model.ReplyMessageRequest
import com.linecorp.bot.webhook.model.FollowEvent
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap

@Service
class MessageService(
    val jsonTemplateService: JSONTemplateService,
    val messagingApiClient: MessagingApiClient
) {
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

    fun processTemplateAndMakeMessage(template: String, modelMap: ModelMap, altText: String): FlexMessage =
        ObjectMapper().run { configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) }
            .readValue(
                """{"type":"flex","altText":"$altText","contents":${
                    jsonTemplateService.processToString(modelMap, template)
                }}""", FlexMessage::class.java
            )
}
