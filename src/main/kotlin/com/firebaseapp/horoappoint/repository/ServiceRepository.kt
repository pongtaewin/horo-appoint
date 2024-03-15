package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.model.Service
import org.springframework.data.repository.CrudRepository

interface ServiceRepository : CrudRepository<Service, Long> {

}