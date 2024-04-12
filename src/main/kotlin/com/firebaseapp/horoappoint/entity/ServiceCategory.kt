package com.firebaseapp.horoappoint.entity

import jakarta.persistence.*
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
        displayImage ?: URL("https://storage.googleapis.com/horo-appoint.appspot.com/banner-default.jpg")

    @JdbcTypeCode(SqlTypes.BOOLEAN)
    @Column(name = "visible", nullable = false)
    var visible: Boolean? = null
}