package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.entity.Service
import com.firebaseapp.horoappoint.entity.ServiceChoice
import org.springframework.data.repository.CrudRepository

interface ServiceChoiceRepository : CrudRepository<ServiceChoice, Long> {
    fun findByServiceAndVisibleTrueOrderByIdAsc(service: Service): List<ServiceChoice>
}
