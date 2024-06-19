package com.firebaseapp.horoappoint.entity

import com.firebaseapp.horoappoint.HoroAppointApplication.Companion.getImg
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
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

    @JdbcTypeCode(SqlTypes.DOUBLE)
    @Column(name = "max_price", nullable = false)
    var maxPrice: Double? = null

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "choices_count", nullable = false)
    var choicesCount: Int? = null

    @Suppress("Unused")
    @JdbcTypeCode(SqlTypes.BOOLEAN)
    @Column(name = "visible", nullable = false)
    var visible: Boolean? = null

    fun getDisplayImageOrDefault() =
        displayImage ?: getImg("banner-default-small.jpg")

    fun getMinPriceRounded(): String =
        DecimalFormat(if (minPrice!! == minPrice!!.roundToLong().toDouble()) "#,##0" else "#,##0.00").format(minPrice!!)

    @Suppress("Unused")
    fun getMaxPriceRounded(): String =
        DecimalFormat(if (maxPrice!! == maxPrice!!.roundToLong().toDouble()) "#,##0" else "#,##0.00").format(maxPrice!!)
}
