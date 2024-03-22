package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.model.CustomerSelection
import org.springframework.data.repository.CrudRepository

interface CustomerSelectionRepository : CrudRepository<CustomerSelection, Long> {

}