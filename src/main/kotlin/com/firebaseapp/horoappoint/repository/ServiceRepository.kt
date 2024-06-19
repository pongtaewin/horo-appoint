package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.entity.Service
import com.firebaseapp.horoappoint.entity.ServiceCategory
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ServiceRepository : CrudRepository<Service, Long> {
    fun findByCategoryAndVisibleTrueOrderByIdAsc(category: ServiceCategory): List<Service>
}
