package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.HoroAppointApplication
import com.firebaseapp.horoappoint.repository.ServiceChoiceRepository
import com.firebaseapp.horoappoint.repository.ServiceCategoryRepository
import com.firebaseapp.horoappoint.repository.ServiceRepository
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.ReplyEvent
import org.slf4j.LoggerFactory
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

    fun <T> handleCatalogEvent(event: T, params: Map<String, String>) where T: Event, T: ReplyEvent {
        val categories = serviceCategoryRepository.findAll()

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/catalog.txt",
                ModelMap().apply {
                    addAttribute("categories", categories.map { category ->
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

    private val log = LoggerFactory.getLogger(HoroAppointApplication::class.java)

    fun <T> handleServiceEvent(event: T, params: Map<String, String>) where T: Event, T: ReplyEvent {
        log.warn(params.toString())
        val category = params["id"]!!.toLong()
        val services = serviceRepository.findByCategory_Id(category)

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/service.txt",
                ModelMap().apply {
                    addAttribute("services", services.map { service ->
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
                "เลือกบริการที่ต้องการ"
            )
        )
    }

    fun <T> handleServiceChoiceEvent(event: T, params: Map<String, String>) where T: Event, T: ReplyEvent {
        val service = serviceRepository.findById(params["id"]!!.toLong()).get()
        val choices = serviceChoiceRepository.findByService(service)

        if (service.choicesCount!! == 1) {
            assert(choices.size == 1)
            customerInfoService.handleServiceSelectedEvent(event, mapOf("choice" to choices[0].id!!.toString()))
        } else messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/service_choice.txt",
                ModelMap().apply {
                    addAttribute("choices", choices.map { choice ->
                        mapOf(
                            "id" to choice.id!!,
                            "name" to choice.name!!,
                            "desc" to choice.description!!,
                            "price" to choice.getPriceRounded()
                        )
                    })
                    addAttribute(
                        "service", mapOf(
                            "id" to service.id!!,
                            "name" to service.name!!,
                            "desc" to service.description!!,
                            "price" to service.getMinPriceRounded(),
                            "image" to service.getDisplayImageOrDefault()
                        )
                    )
                },
                "เลือกบริการที่ต้องการ"
            )
        )
    }

    fun <T> handleEvent(event: T, query: String, params: Map<String, String>) where T : Event, T: ReplyEvent {
        when (query) {
            OPEN_CATALOG -> handleCatalogEvent(event, params)
            SELECT_SERVICE -> handleServiceEvent(event, params)
            SELECT_SERVICE_CHOICE -> handleServiceChoiceEvent(event, params)
        }
    }

    companion object {
        const val OPEN_CATALOG = "openCatalog"
        const val SELECT_SERVICE = "selectService"
        const val SELECT_SERVICE_CHOICE = "selectServiceChoice"
        val PARAMS = listOf(OPEN_CATALOG, SELECT_SERVICE, SELECT_SERVICE_CHOICE)
    }

}