package com.firebaseapp.horoappoint

import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.stereotype.Service

@Service
interface LineBotEventService {
    abstract val PARAMS: List<String>
    abstract fun <T> handleEvent(event: T, query: String, params: Map<String, String>) where T : Event, T : ReplyEvent
}
