package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.repository.ServiceCategoryRepository
import com.firebaseapp.horoappoint.repository.ServiceRepository
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.PostbackEvent
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap

private const val STR_CHOOSE_SERVICE = "เลือกบริการที่ต้องการ"
private const val STR_CHANGE_SERVICE_GROUP = "เปลี่ยนกลุ่มบริการ"

@Service
class CatalogService(
    private val messageService: MessageService,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository
) {
    @Suppress("UnusedParameter")
    fun handleServiceCategoryEvent(event: PostbackEvent, params: Map<String, String>) {
        val categories = serviceCategoryRepository.findAllByVisibleTrueOrderByIdAsc()

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/service_category.txt",
                ModelMap().apply {
                    put(
                        "categories",
                        categories.map { category ->
                            mapOf(
                                "id" to category.id!!,
                                "name" to category.name!!,
                                "desc" to category.description!!,
                                "image" to category.getDisplayImageOrDefault()
                            )
                        }
                    )
                },
                STR_CHOOSE_SERVICE
            )
        )
    }

    fun <T> handleServiceEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val category = serviceCategoryRepository.findById((params["id"] ?: error("Null argument 'id'")).toLong()).get()
        val services = serviceRepository.findByCategoryAndVisibleTrueOrderByIdAsc(category)

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/service.txt",
                ModelMap().apply {
                    this["category"] = mapOf(
                        "id" to category.id!!,
                        "name" to category.name!!,
                        "desc" to category.description!!,
                        "image" to category.getDisplayImageOrDefault()
                    )
                    this["services"] = services.map { service ->
                        mapOf(
                            "id" to service.id!!,
                            "name" to service.name!!,
                            "desc" to service.description!!,
                            "price" to service.getMinPriceRounded(),
                            "choices" to service.choicesCount!!,
                            "image" to service.getDisplayImageOrDefault(),
                            "same_price" to (service.minPrice == service.maxPrice)
                        )
                    }
                },
                STR_CHOOSE_SERVICE,
                messageService.quickReplyOf("category.png", STR_CHANGE_SERVICE_GROUP, "serviceCategory")
            )
        )
    }
}
