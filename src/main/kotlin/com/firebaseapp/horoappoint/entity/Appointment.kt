package com.firebaseapp.horoappoint.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
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

    @ManyToOne
    @JoinColumn(name = "location_id")
    var location: Location? = null

    @OneToOne(orphanRemoval = false)
    @JoinColumn(name = "timeframe_id")
    var timeframe: Timeframe? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "slip_image")
    var slipImage: URL? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "selection_added", nullable = false)
    var selectionAdded: Instant? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "selection_final")
    var selectionFinal: Instant? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "slip_added")
    var slipAdded: Instant? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "slip_final")
    var slipFinal: Instant? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "approved")
    var approved: Instant? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "finished")
    var finished: Instant? = null

    fun getLocationDescriptor(mini: Boolean = false): String = when (serviceChoice!!.serviceType!!) {
        ServiceType.ONLINE_CHAT, ServiceType.PASSIVE -> "ผ่านทางแชทไลน์"
        ServiceType.ON_PREMISE, ServiceType.GUIDE ->
            if (mini) "ที่สำนักสักฯ" else "สำนักสักลายมือเศรษฐี จอมพล 789\\nต.ท้ายหาด อ.เมือง จ.สมุทรสงคราม"

        ServiceType.MEETUP -> location!!.getName(mini)
    }

    fun getTimeLastUpdated(): Instant =
        listOfNotNull(selectionAdded, selectionFinal, slipAdded, slipFinal, approved, finished).maxOrNull()!!
}
