package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.model.Customer
import com.firebaseapp.horoappoint.model.CustomerSelection
import org.springframework.data.repository.CrudRepository
import java.util.*

interface CustomerSelectionRepository : CrudRepository<CustomerSelection, Long> {


    fun findByCustomer_LineUID(lineUID: String): Optional<CustomerSelection>

    fun findByCustomer(customer: Customer): Optional<CustomerSelection>


}