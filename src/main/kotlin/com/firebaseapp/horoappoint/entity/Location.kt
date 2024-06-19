package com.firebaseapp.horoappoint.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

private const val EARTH_RADIUS: Double = 6371.0
private const val DEGREE_PER_RADIAN: Double = PI / 180.0
private const val LAT_SHOP = 13.414733 * DEGREE_PER_RADIAN
private const val LON_SHOP = 99.987572 * DEGREE_PER_RADIAN
private const val ZONE_1_CUTOFF: Double = 50.0
private const val ZONE_2_CUTOFF: Double = 150.0
private const val ZONE_3_CUTOFF: Double = 2_000.0

@Entity
@Table(name = "location")
class Location {
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

    fun getName(mini: Boolean = false): String = if (!mini) {
        "$subdistrict $district $province"
    } else {
        "${subdistrict!!.replace("ตำบล", "ต.")} ${district!!.replace("อำเภอ", "อ.")} ${
            province!!.replace("จังหวัด", "จ.").replace("กรุงเทพมหานคร", "กทม.")
        }"
    }

    fun distanceFromShop(): Double = EARTH_RADIUS * acos(
        sin(latitude!! * DEGREE_PER_RADIAN) * sin(LAT_SHOP) +
            cos(latitude!! * DEGREE_PER_RADIAN) * cos(LAT_SHOP) * cos(LON_SHOP - longitude!! * DEGREE_PER_RADIAN)
    )

    @Suppress("MagicNumber")
    fun calculateZone(): Int = when (val dist = distanceFromShop()) {
        in 0.0..<ZONE_1_CUTOFF -> 1
        in ZONE_1_CUTOFF..<ZONE_2_CUTOFF -> 2
        in ZONE_2_CUTOFF..<ZONE_3_CUTOFF -> 3
        else -> error("Invalid distance $dist")
    }

    fun provinceMini(): String = province!!.replace("จังหวัด", "")
    fun districtMini(): String = district!!.replace("อำเภอ", "").replace("เขต", "")
    fun subdistrictMini(): String = subdistrict!!.replace("ตำบล", "").replace("แขวง", "")
}
