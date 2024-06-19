package com.firebaseapp.horoappoint.entity

import com.firebaseapp.horoappoint.HoroAppointApplication.Companion.getImg
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.net.URL

@Entity
@Table(name = "service_category")
class ServiceCategory {
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

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "display_image")
    var displayImage: URL? = null

    fun getDisplayImageOrDefault() =
        displayImage ?: getImg("banner-default.jpg")

    @Suppress("Unused")
    @JdbcTypeCode(SqlTypes.BOOLEAN)
    @Column(name = "visible", nullable = false)
    var visible: Boolean? = null
}
