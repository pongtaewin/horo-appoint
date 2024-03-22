package com.firebaseapp.horoappoint.model

import com.firebaseapp.horoappoint.model.enums.DurationType
import com.firebaseapp.horoappoint.model.enums.ServiceType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "service")
class Service {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id", nullable = false)
    var id: Long? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "name", nullable = false)
    var name: String? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "description", nullable = false)
    var description: String? = null

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "service_type", nullable = false)
    var serviceType: ServiceType? = null //todo add to Database

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "duration_type", nullable = false)
    var durationType: DurationType? = null

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "duration_minutes")
    var durationMinutes: Int? = null

    fun getDurationText(): String {
        return when (durationType!!) {
            DurationType.ALL_DAY -> "ตลอดทั้งวัน"
            DurationType.TIMED -> with(durationMinutes!!) {
                when {
                    this < 60 -> "$this นาที"
                    this % 60 == 0 -> "${this / 60} ชั่วโมง"
                    else -> "${this / 60} ชั่วโมง ${this % 60} นาที"
                }
            }
        }
    }
}