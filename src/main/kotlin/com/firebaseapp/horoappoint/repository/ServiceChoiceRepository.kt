package com.firebaseapp.horoappoint.repository;

import com.firebaseapp.horoappoint.model.Service
import com.firebaseapp.horoappoint.model.ServiceChoice
import org.springframework.data.repository.CrudRepository

interface ServiceChoiceRepository : CrudRepository<ServiceChoice, Long> {
    fun findByServiceOrderByIdAsc(service: Service): List<ServiceChoice>
}