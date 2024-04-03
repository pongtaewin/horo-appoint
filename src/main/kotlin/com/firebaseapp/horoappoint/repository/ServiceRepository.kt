package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.model.Service
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ServiceRepository : CrudRepository<Service, Long> {
    fun findByCategory_Id(id: Long): List<Service>
}