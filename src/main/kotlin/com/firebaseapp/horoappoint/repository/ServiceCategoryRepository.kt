package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.model.ServiceCategory
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ServiceCategoryRepository : CrudRepository<ServiceCategory, Long> {


    fun findAllByOrderByIdAsc(): List<ServiceCategory>
}