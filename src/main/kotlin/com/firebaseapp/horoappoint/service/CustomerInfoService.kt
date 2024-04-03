package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.model.CustomerSelection
import com.firebaseapp.horoappoint.model.ServiceChoice
import com.firebaseapp.horoappoint.repository.CustomerRepository
import com.firebaseapp.horoappoint.repository.CustomerSelectionRepository
import com.firebaseapp.horoappoint.repository.ServiceChoiceRepository
import com.linecorp.bot.messaging.model.TextMessage
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.stereotype.Service
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
        val selection = getOrCreateCustomerSelection(event, choice)
        messageService.replyMessage(
            event,
            TextMessage("ได้ทำการบันทึกข้อมูลเรียบร้อย"),
            TextMessage(selection.run{
                """
                    id = $id
                    customer = [name = ${customer?.displayName}, uid = ${customer?.id}]
                    selection = [name = ${serviceChoice?.service?.name}, option = ${serviceChoice?.name}, id = ${serviceChoice?.id}]
                """.trimIndent()
            })
        )
        /* todo Figure Tasks by Repostiory */
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

    companion object {
        const val SERVICE_SELECTED = "serviceSelected"
        val PARAMS = listOf(SERVICE_SELECTED)
    }

}