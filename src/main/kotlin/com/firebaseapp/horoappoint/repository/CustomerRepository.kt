package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.entity.Customer
import com.linecorp.bot.webhook.model.Event
import org.springframework.data.repository.CrudRepository
import java.util.*

interface CustomerRepository : CrudRepository<Customer, String> {

    fun findByLineUID(lineUID: String): Optional<Customer>

    fun findByEvent(event: Event): Optional<Customer> = findByLineUID(event.source().userId())
}
