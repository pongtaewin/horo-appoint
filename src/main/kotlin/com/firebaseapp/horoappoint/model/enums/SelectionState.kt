package com.firebaseapp.horoappoint.model.enums

import com.firebaseapp.horoappoint.model.CustomerSelection

enum class SelectionState {
    LOCATION_REQUIRED,
    DATE_REQUIRED,
    TIME_REQUIRED,
    CUSTOMER_NAME_REQUIRED,
    READY;

    companion object {
        fun CustomerSelection.checkSelectionState(): SelectionState = when {
            // Step 1 กำหนดสถานที่
            location == null && serviceChoice!!.serviceType!! == ServiceType.MEETUP -> LOCATION_REQUIRED
            // Step 2 เลือกวันที่
            startDate == null -> DATE_REQUIRED
            // Step 3 เลือกเวลา
            startTime == null -> TIME_REQUIRED
            // Step 4 ชื่อลูกค้า
            customer!!.fullName == null -> CUSTOMER_NAME_REQUIRED
            // Step 5 ยืนยันการจอง อัปขึ้นระบบ
            else -> READY
        }
    }

}
