package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.LineBotEventService
import com.firebaseapp.horoappoint.model.CustomerSelection
import com.firebaseapp.horoappoint.model.ServiceChoice
import com.firebaseapp.horoappoint.model.enums.SelectionState
import com.firebaseapp.horoappoint.repository.CustomerRepository
import com.firebaseapp.horoappoint.repository.CustomerSelectionRepository
import com.firebaseapp.horoappoint.repository.ServiceChoiceRepository
import com.linecorp.bot.messaging.model.*
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import kotlin.jvm.optionals.getOrElse

@Service
class CustomerInfoService(
    private val messageService: MessageService,
    private val serviceChoiceRepository: ServiceChoiceRepository,
    private val customerSelectionRepository: CustomerSelectionRepository,
    private val customerRepository: CustomerRepository,
) {
    fun <T> handleServiceSelectedEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val choice = serviceChoiceRepository.findById(params["choice"]!!.toLong()).get()

        println("Service Selected = $choice")
        // Handle Switched State
        val selection = getOrCreateCustomerSelection(event, choice)
        val message: Message = when (selection.getSelectionState()) {
            SelectionState.LOCATION_REQUIRED ->
                messageService.processTemplateAndMakeMessage(
                    "json/prompt_customer_location.txt",
                    ModelMap(),
                    "กรุณาเลือกสถานที่ให้บริการ",
                    QuickReplyItem(LocationAction("เลือกตำแหน่งบนแผนที่"))
                )


            SelectionState.DATE_REQUIRED -> messageService.processTemplateAndMakeMessage(
                "json/pick_date.txt",
                ModelMap(),
                "กรุณาเลือกวันที่"
            )

            SelectionState.TIME_REQUIRED -> messageService.processTemplateAndMakeMessage(
                "json/pick_date.txt",
                ModelMap(),
                "กรุณาเลือกเวลารับบริการ"
            )

            SelectionState.CUSTOMER_NAME_REQUIRED -> messageService.processTemplateAndMakeMessage(
                "json/enter_name.txt",
                ModelMap(),
                "กรุณาเลือกเวลารับบริการ",
                QuickReplyItem(PostbackAction("เปลี่ยนวันรับบริการ", "data", null, null, null, null)),
                QuickReplyItem(PostbackAction("เปลี่ยนเวลารับบริการ", "data", null, null, null, null))
            )

            SelectionState.READY -> messageService.processTemplateAndMakeMessage(
                "json/confirmation.txt",
                ModelMap(),
                "กรุณายืนยันออเดอร์",

                QuickReplyItem(PostbackAction("เปลี่ยนบริการ", "data", null, null, null, null)),
                QuickReplyItem(PostbackAction("เปลี่ยนวันรับบริการ", "data", null, null, null, null)),
                QuickReplyItem(PostbackAction("เปลี่ยนเวลารับบริการ", "data", null, null, null, null)),
                QuickReplyItem(PostbackAction("เปลี่ยนชื่อ-นามสกุล", "data", null, null, null, null))
            )
        }

        messageService.replyMessage(event, message)

    }

    fun <T> getOrCreateCustomerSelection(
        event: T,
        choice: ServiceChoice
    ): CustomerSelection where T : Event, T : ReplyEvent {
        val customer = customerRepository.findByLineUID(event.source().userId()).get()
        return customerSelectionRepository.findByCustomer(customer).getOrElse {
            customerSelectionRepository.save(CustomerSelection().apply {
                this.customer = customer
                serviceChoice = choice
            })
        }.run {
            if (serviceChoice != choice) customerSelectionRepository.save(apply { serviceChoice = choice }) else this
        }
    }

    fun <T> handleEvent(event: T, query: String, params: Map<String, String>) where T : Event, T : ReplyEvent {
        when (query) {
            SERVICE_SELECTED -> handleServiceSelectedEvent(event, params)
        }
    }

    val PARAMS = listOf(SERVICE_SELECTED, NAME_RECEIVED)

    companion object {
        const val SERVICE_SELECTED = "serviceSelected"
        const val NAME_RECEIVED = "nameReceived"
    }

}