package com.firebaseapp.horoappoint.model

import com.firebaseapp.horoappoint.model.enums.ServiceType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.net.URL
import java.time.Instant


@Entity
@Table(name = "appointment")
class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id", nullable = false)
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: Customer? = null

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_choice_id", nullable = false)
    var serviceChoice: ServiceChoice? = null

    @OneToOne(orphanRemoval = true)
    @JoinColumn(name = "timeframe_id")
    var timeframe: Timeframe? = null

    @ManyToOne
    @JoinColumn(name = "location_id")
    var location: Location? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_image")
    var paymentImage: URL? = null

    @JdbcTypeCode(SqlTypes.BOOLEAN)
    @Column(name = "is_slip_final", nullable = false)
    var isSlipFinal: Boolean? = false

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created", nullable = false)
    var created: Instant? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "uploaded")
    var uploaded: Instant? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "approved")
    var approved: Instant? = null

    fun getLocationDescriptor(): String = when (serviceChoice!!.serviceType!!) {
        ServiceType.ONLINE_CHAT, ServiceType.PASSIVE -> "ผ่านทางแชทไลน์"
        ServiceType.ON_PREMISE, ServiceType.GUIDE -> "สำนักสักลายมือเศรษฐี จอมพล 789\nตำบลท้ายหาด อำเภอเมือง จังหวัดสมุทรสงคราม"
        ServiceType.MEETUP -> location!!.getName()
    }
}
