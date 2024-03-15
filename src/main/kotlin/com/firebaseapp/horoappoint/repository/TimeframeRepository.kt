package com.firebaseapp.horoappoint.repository

import com.firebaseapp.horoappoint.model.Timeframe
import org.springframework.data.repository.CrudRepository

interface TimeframeRepository : CrudRepository<Timeframe, Long> {

}