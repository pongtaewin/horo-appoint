package com.firebaseapp.horoappoint.model

import com.firebaseapp.horoappoint.model.enums.DurationType
import com.firebaseapp.horoappoint.model.enums.ServiceType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import kotlin.math.roundToLong

@Entity
@Table(name = "service_choice")
class ServiceChoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "name", nullable = false)
    var name: String? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "description", nullable = false)
    var description: String? = null

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    var service: Service? = null

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "service_type", nullable = false)
    var serviceType: ServiceType? = null

    @JdbcTypeCode(SqlTypes.DOUBLE)
    @Column(name = "price", nullable = false)
    var price: Double? = null

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "duration_type", nullable = false)
    var durationType: DurationType? = null

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "duration_minutes")
    var durationMinutes: Int? = null

    fun getPriceRounded() = String.format(
        if (price!! == price!!.roundToLong().toDouble()) "%.0f" else "%.2f",
        price!!
    )

    fun getDurationText(): String = when (durationType!!) {
        DurationType.ALL_DAY -> "ตลอดทั้งวัน"
        DurationType.TIMED -> listOf(durationMinutes!! / 60 to "ชั่วโมง", durationMinutes!! % 60 to "นาที")
            .filter { it.first != 0 }.joinToString(" ") { (a, b) -> "$a $b" }
    }

    fun getFullDescription(): String = service!!.description!! +
            if (service!!.choicesCount != 1) "\n\n" + description else ""
}