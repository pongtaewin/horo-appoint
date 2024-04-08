package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.LineBotEventService
import com.firebaseapp.horoappoint.repository.ServiceChoiceRepository
import com.firebaseapp.horoappoint.repository.ServiceCategoryRepository
import com.firebaseapp.horoappoint.repository.ServiceRepository
import com.linecorp.bot.messaging.model.PostbackAction
import com.linecorp.bot.messaging.model.QuickReplyItem
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap

@Service
class CatalogService(
    val messageService: MessageService,
    val customerInfoService: CustomerInfoService,
    val serviceRepository: ServiceRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceChoiceRepository: ServiceChoiceRepository
) {


    //todo ServiceGroup -> ServiceCategory
    fun <T> handleServiceCategoryEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val categories = serviceCategoryRepository.findAllByOrderByIdAsc()

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
        val services = serviceRepository.findByCategoryOrderByIdAsc(category)

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
                            "image" to service.getDisplayImageOrDefault()
                        )
                    })
                },
                "เลือกบริการที่ต้องการ",
                QuickReplyItem(
                    PostbackAction(
                        "เปลี่ยนหมวดหมู่บริการ",
                        "selectServiceCategory",
                        null, null, null, null
                    )
                )
            )
        )
    }

    fun <T> handleServiceChoiceEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val service = serviceRepository.findById(params["id"]!!.toLong()).get()
        val choices = serviceChoiceRepository.findByServiceOrderByIdAsc(service)
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
                            "image" to service.getDisplayImageOrDefault()
                        )
                    )
                    put("single", service.choicesCount == 1)
                },
                "เลือกบริการที่ต้องการ",
                QuickReplyItem(
                    PostbackAction(
                        "เปลี่ยนหมวดหมู่บริการ",
                        "selectServiceCategory",
                        null, null, null, null
                    )
                ),
                QuickReplyItem(
                    PostbackAction(
                        "เปลี่ยนบริการ",
                        "selectService?id=${service.category!!.id!!}",
                        null, null, null, null
                    )
                )
            )
        )
    }

    fun <T> handleEvent(event: T, query: String, params: Map<String, String>) where T : Event, T : ReplyEvent {
        when (query) {
            SELECT_SERVICE_CATEGORY -> handleServiceCategoryEvent(event, params)
            SELECT_SERVICE -> handleServiceEvent(event, params)
            SELECT_SERVICE_CHOICE -> handleServiceChoiceEvent(event, params)
        }
    }

    val PARAMS = listOf(SELECT_SERVICE_CATEGORY, SELECT_SERVICE, SELECT_SERVICE_CHOICE)

    companion object {
        const val SELECT_SERVICE_CATEGORY = "selectServiceCategory"
        const val SELECT_SERVICE = "selectService"
        const val SELECT_SERVICE_CHOICE = "selectServiceChoice"
    }

}