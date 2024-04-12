package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.entity.Location
import org.springframework.data.repository.CrudRepository

interface LocationRepository : CrudRepository<Location, Long> {
}