package com.firebaseapp.horoappoint.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.sql.Timestamp
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
    @JoinColumn(name = "service_id", nullable = false)
    var service: Service? = null

    @OneToOne(orphanRemoval = true)
    @JoinColumn(name = "timeframe_id", nullable = false)
    var timeframe: Timeframe? = null

    @ManyToOne
    @JoinColumn(name = "location_id")
    var location: Location? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "last_updated", nullable = false)
    var updated: Instant? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created", nullable = false)
    var created: Instant? = null

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "approved")
    var approved: Instant? = null


}
