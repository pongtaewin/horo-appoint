package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.entity.Appointment
import com.linecorp.bot.webhook.model.Event
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface AppointmentRepository : CrudRepository<Appointment, Long> {

    override fun findById(id: Long): Optional<Appointment>

    @Query("select a from Appointment a where a.customer.lineUID = ?1")
    fun findByCustomerLineUID(lineUID: String): Optional<Appointment>

    fun findByEvent(event: Event) = findByCustomerLineUID(event.source().userId())

    @Query(
        """select a from Appointment a
where a.customer.fullName like concat('%', ?1, '%') 
or a.customer.displayName like concat('%', ?1, '%') 
or a.serviceChoice.service.name like concat('%', ?1, '%') 
or a.serviceChoice.name like concat('%', ?1, '%')"""
    )
    fun findByMatchingQueryOnCustomerOrService(query: String): List<Appointment>
}
