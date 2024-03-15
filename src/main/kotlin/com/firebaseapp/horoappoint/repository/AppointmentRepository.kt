package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.model.Appointment
import org.springframework.data.repository.CrudRepository

interface AppointmentRepository : CrudRepository<Appointment, Long> {

}