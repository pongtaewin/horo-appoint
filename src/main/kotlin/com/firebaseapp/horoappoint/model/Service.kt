package com.firebaseapp.horoappoint.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.net.URL
import java.text.DecimalFormat
import kotlin.math.roundToLong

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

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "display_image")
    var displayImage: URL? = null

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    var category: ServiceCategory? = null

    @JdbcTypeCode(SqlTypes.DOUBLE)
    @Column(name = "min_price", nullable = false)
    var minPrice: Double? = null

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "choices_count", nullable = false)
    var choicesCount: Int? = null

    fun getDisplayImageOrDefault() =
        displayImage ?: URL("https://storage.googleapis.com/horo-appoint.appspot.com/banner-default-small.jpg")

    fun getMinPriceRounded(): String =
        DecimalFormat(if (minPrice!! == minPrice!!.roundToLong().toDouble()) "#,##0" else "#,##0.00").format(minPrice!!)
}