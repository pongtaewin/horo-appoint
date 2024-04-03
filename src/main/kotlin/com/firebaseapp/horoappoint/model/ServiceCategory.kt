package com.firebaseapp.horoappoint.model

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

    fun getDisplayImageOrDefault() = displayImage ?: URL(
        "https://images.unsplash.com/photo-1528222354212-a29573cdb844" +
                "?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=869&q=80"
    )

}