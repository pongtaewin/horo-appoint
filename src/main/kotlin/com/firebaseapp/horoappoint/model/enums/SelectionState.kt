package com.firebaseapp.horoappoint.model.enums

import com.firebaseapp.horoappoint.model.CustomerSelection

enum class SelectionState {
    CUSTOMER_NAME_REQUIRED,
    CUSTOMER_LOCATION_REQUIRED,
    STAFF_LOCATION_REQUIRED,
    DATE_REQUIRED,
    TIME_REQUIRED,
    READY;

    companion object {
        fun checkSelectionState(selection: CustomerSelection): SelectionState = with(selection) {
            // Step 1 ชื่อลูกค้า
            if(customer!!.getFullName() == null) return CUSTOMER_NAME_REQUIRED

            // Step 2 กำหนดสถานที่
            when (val type = serviceChoice!!.serviceType!!) {
                ServiceType.MEETUP -> {
                    if (customerLocation == null) return CUSTOMER_LOCATION_REQUIRED
                }

                ServiceType.ON_PREMISE, ServiceType.GUIDE -> {
                    if (staffLocation == null) return STAFF_LOCATION_REQUIRED
                    if (type == ServiceType.ON_PREMISE != staffLocation!!.isOwned!!)
                        throw IllegalStateException("Staff Location Mismatch. Owned=${staffLocation!!.isOwned!!},ServiceType={$type}")
                }

                ServiceType.ONLINE_CHAT -> {}
            }

            return when {
                // Step 3 เลือกวันที่
                startDate == null -> DATE_REQUIRED
                // Step 4 เลือกเวลา
                startTime == null -> TIME_REQUIRED
                // Step 5 ยืนยันการจอง อัปขึ้นระบบ
                else -> READY
            }
        }
    }
}
