package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.model.Customer
import org.springframework.data.repository.CrudRepository
import java.util.*

interface CustomerRepository : CrudRepository<Customer, String> {


    fun findByLineUID(lineUID: String): Optional<Customer>


}