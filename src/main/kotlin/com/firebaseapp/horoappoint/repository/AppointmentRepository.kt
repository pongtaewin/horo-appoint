package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.model.Appointment
import com.linecorp.bot.webhook.model.Event
import org.springframework.data.repository.CrudRepository
import java.util.*

interface AppointmentRepository : CrudRepository<Appointment, Long> {


    override fun findById(id: Long): Optional<Appointment>


    fun findByEvent(event: Event) = findById(event.source().userId().toLong())
}