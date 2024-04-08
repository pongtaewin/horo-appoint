package com.firebaseapp.horoappoint.model

import com.firebaseapp.horoappoint.model.enums.ServiceType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.text.DecimalFormat
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

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "duration_days")
    var durationDays: Int? = null

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "duration_minutes")
    var durationMinutes: Int? = null

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "padding_before_minutes")
    var paddingBeforeMinutes: Int? = null

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "padding_after_minutes")
    var paddingAfterMinutes: Int? = null

    fun getPriceRounded(): String =
        DecimalFormat(if (price!! == price!!.roundToLong().toDouble()) "#,##0" else "#,##0.00").format(price!!)

    fun getDurationText(): String = when (durationDays) {
        null -> listOf(durationMinutes!! / 60 to "ชั่วโมง", durationMinutes!! % 60 to "นาที")
            .filter { it.first != 0 }.joinToString(" ") { (a, b) -> "$a $b" }

        0 -> "ภายใน 7-15 วัน"
        1 -> "ตลอดวัน"
        else -> "${durationDays!!} วัน ${durationDays!! - 1} คืน"
    }

    fun getLocationText(): String = when (serviceType!!) {
        ServiceType.ONLINE_CHAT, ServiceType.PASSIVE -> "ผ่านทางแชทไลน์"
        ServiceType.ON_PREMISE -> "ผ่านทางสำนักฯ"
        ServiceType.MEETUP -> "เลือกได้ตามต้องการ"
        ServiceType.GUIDE -> "ตามที่ระบุไว้ในบริการ"
    }

    fun getFullDescription(): String = service!!.description!! +
            if (service!!.choicesCount != 1) "\n\n" + description else ""
}