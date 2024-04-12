package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.entity.Appointment
import com.linecorp.bot.webhook.model.Event
import org.springframework.data.repository.CrudRepository
import java.util.*

interface AppointmentRepository : CrudRepository<Appointment, Long> {



    override fun findById(id: Long): Optional<Appointment>


    fun findByCustomer_LineUID(lineUID: String): Optional<Appointment>

    fun findByEvent(event: Event) = findByCustomer_LineUID(event.source().userId())


}