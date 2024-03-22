package com.firebaseapp.horoappoint.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "location")
class CustomerLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id", nullable = false)
    var id: Long? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "province", nullable = false)
    var province: String? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "district", nullable = false)
    var district: String? = null

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "subdistrict", nullable = false)
    var subdistrict: String? = null

    @JdbcTypeCode(SqlTypes.FLOAT)
    @Column(name = "latitude", nullable = false)
    var latitude: Double? = null

    @JdbcTypeCode(SqlTypes.FLOAT)
    @Column(name = "longitude", nullable = false)
    var longitude: Double? = null

    fun getName(): String = "$subdistrict $district $province"
}