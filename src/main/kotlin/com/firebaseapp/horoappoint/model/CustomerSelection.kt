package com.firebaseapp.horoappoint.model

import com.firebaseapp.horoappoint.model.enums.SelectionState
import com.firebaseapp.horoappoint.model.enums.ServiceType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.lang.NonNull
import org.springframework.lang.Nullable
import java.time.Instant


// todo Create Table
@Entity
@Table(name = "customer_selection")
class CustomerSelection {
    @Id
    @NonNull
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @NonNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: Customer? = null

    @NonNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "service_choice_id", nullable = false)
    var serviceChoice: ServiceChoice? = null

    @Nullable
    @ManyToOne
    @JoinColumn(name = "customer_location_id")
    var customerLocation: CustomerLocation? = null

    @Nullable
    @ManyToOne
    @JoinColumn(name = "staff_location_id")
    var staffLocation: StaffLocation? = null

    @Nullable
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "start_date")
    var startDate: Instant? = null

    @Nullable
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "start_time")
    var startTime: Instant? = null


    fun getLocationDescriptor(): String = when (serviceChoice!!.serviceType!!) {
        ServiceType.ONLINE_CHAT -> "ผ่านทางแชทไลน์"
        ServiceType.MEETUP -> customerLocation!!.getName()
        ServiceType.ON_PREMISE, ServiceType.GUIDE -> staffLocation!!.fullName!!
    }

    fun getSelectionState() = SelectionState.checkSelectionState(this)
}
