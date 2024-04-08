package com.firebaseapp.horoappoint.model

import com.firebaseapp.horoappoint.model.enums.SelectionState.Companion.checkSelectionState
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
    @JoinColumn(name = "location_id")
    var location: Location? = null

    @Nullable
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "start_date")
    var startDate: Instant? = null

    @Nullable
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "start_time")
    var startTime: Instant? = null

    @NonNull
    @JdbcTypeCode(SqlTypes.BOOLEAN)
    @Column(name = "is_waiting_for_name", nullable = false)
    var isWaitingForName: Boolean? = false

    @NonNull
    @JdbcTypeCode(SqlTypes.BOOLEAN)
    @Column(name = "is_location_confirmed", nullable = false)
    var isLocationConfirmed: Boolean? = false
    //todo add to db

    fun getLocationDescriptor(): String = when (serviceChoice!!.serviceType!!) {
        ServiceType.ONLINE_CHAT, ServiceType.PASSIVE -> "ผ่านทางแชทไลน์"
        ServiceType.ON_PREMISE, ServiceType.GUIDE -> "สำนักสักลายมือเศรษฐี จอมพล 789\nตำบลท้ายหาด อำเภอเมือง จังหวัดสมุทรสงคราม"
        ServiceType.MEETUP -> location!!.getName()
    }

    fun getSelectionState() = checkSelectionState()
}
