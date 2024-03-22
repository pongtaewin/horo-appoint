package com.firebaseapp.horoappoint.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.net.URL

@Entity
@Table(name = "staff_location")
class StaffLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "name", nullable = false)
    var name: String? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "desc", nullable = false)
    var desc: String? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "map_link", nullable = false)
    var mapLink: URL? = null

    @JdbcTypeCode(SqlTypes.BOOLEAN)
    @Column(name = "is_owned", nullable = false)
    var isOwned: Boolean? = null
}