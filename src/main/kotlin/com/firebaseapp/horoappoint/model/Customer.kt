package com.firebaseapp.horoappoint.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.net.URL


// UserID is associated with the setting
@Entity
class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id", nullable = false)
    val id: Long? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "line_uid", nullable = false)
    var lineUID: String? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "display_name", nullable = false)
    var displayName: String? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "full_name")
    var fullName: String? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "display_image")
    var displayImage: URL? = null

}
