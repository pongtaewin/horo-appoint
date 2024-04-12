package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.repository.ServiceCategoryRepository
import com.firebaseapp.horoappoint.repository.ServiceChoiceRepository
import com.firebaseapp.horoappoint.repository.ServiceRepository
import com.linecorp.bot.messaging.model.PostbackAction
import com.linecorp.bot.messaging.model.QuickReplyItem
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import java.net.URI

@Service
class CatalogService(
    private val messageService: MessageService,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
    private val serviceChoiceRepository: ServiceChoiceRepository
) {
    fun <T> handleServiceCategoryEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val categories = serviceCategoryRepository.findAllByVisibleTrueOrderByIdAsc()

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/service_category.txt",
                ModelMap().apply {
                    put("categories", categories.map { category ->
                        mapOf(
                            "id" to category.id!!,
                            "name" to category.name!!,
                            "desc" to category.description!!,
                            "image" to category.getDisplayImageOrDefault()
                        )
                    })
                },
                "เลือกบริการที่ต้องการ"
            )
        )
    }

    fun <T> handleServiceEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val category = serviceCategoryRepository.findById(params["id"]!!.toLong()).get()
        val services = serviceRepository.findByCategoryAndVisibleTrueOrderByIdAsc(category)

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/service.txt",
                ModelMap().apply {
                    put(
                        "category", mapOf(
                            "id" to category.id!!,
                            "name" to category.name!!,
                            "desc" to category.description!!,
                            "image" to category.getDisplayImageOrDefault()
                        )
                    )
                    put("services", services.map { service ->
                        mapOf(
                            "id" to service.id!!,
                            "name" to service.name!!,
                            "desc" to service.description!!,
                            "price" to service.getMinPriceRounded(),
                            "choices" to service.choicesCount!!,
                            "image" to service.getDisplayImageOrDefault(),
                            "same_price" to (service.minPrice == service.maxPrice)
                        )
                    })
                },
                "เลือกบริการที่ต้องการ",
                QuickReplyItem(
                    URI("https://storage.googleapis.com/horo-appoint.appspot.com/category.png"),
                    PostbackAction(
                        "เปลี่ยนกลุ่มบริการ",
                        "serviceCategory",
                        "เปลี่ยนกลุ่มบริการ", null, null, null
                    )
                )
            )
        )
    }

    fun <T> handleServiceChoiceEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val service = serviceRepository.findById(params["id"]!!.toLong()).get()
        val choices = serviceChoiceRepository.findByServiceAndVisibleTrueOrderByIdAsc(service)
        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/service_choice.txt",
                ModelMap().apply {
                    put("choices", choices.map { choice ->
                        mapOf(
                            "id" to choice.id!!,
                            "name" to choice.name!!,
                            "desc" to choice.description!!,
                            "price" to choice.getPriceRounded(),
                            "location" to choice.getLocationText(),
                            "duration" to choice.getDurationText()
                        )
                    })
                    put(
                        "service", mapOf(
                            "id" to service.id!!,
                            "category_id" to service.category!!.id!!,
                            "name" to service.name!!,
                            "desc" to service.description!!,
                            "price" to service.getMinPriceRounded(),
                            "image" to service.getDisplayImageOrDefault(),
                            "same_price" to (service.minPrice != service.maxPrice)
                        )
                    )
                },
                "เลือกบริการที่ต้องการ",
                QuickReplyItem(
                    URI("https://storage.googleapis.com/horo-appoint.appspot.com/category.png"),
                    PostbackAction(
                        "เปลี่ยนกลุ่มบริการ",
                        "serviceCategory",
                        "เปลี่ยนกลุ่มบริการ", null, null, null
                    )
                ),
                QuickReplyItem(
                    URI("https://storage.googleapis.com/horo-appoint.appspot.com/service.png"),
                    PostbackAction(
                        "เปลี่ยนบริการ",
                        "service?id=${service.category!!.id!!}",
                        "เปลี่ยนบริการ", null, null, null
                    )
                )
            )
        )
    }
}