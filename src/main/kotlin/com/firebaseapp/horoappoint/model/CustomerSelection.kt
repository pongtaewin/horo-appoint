package com.firebaseapp.horoappoint.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes


// todo Create Table
@Entity
@Table(name = "customer_selection")
class CustomerSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: Customer? = null

    @OneToOne(optional = false, orphanRemoval = true)
    @JoinColumn(name = "service_id", nullable = false)
    var service: Service? = null

    //todo mandatory if service type is MEETUP
    @OneToOne(orphanRemoval = true)
    @JoinColumn(name = "customer_location_id")
    var customerLocation: CustomerLocation? = null

    //todo mandatory if service type is ON_PREMISE or GUIDE
    @OneToOne(orphanRemoval = true)
    @JoinColumn(name = "staff_location_id")
    var staffLocation: StaffLocation? = null

    @JdbcTypeCode(SqlTypes.DOUBLE)
    @Column(name = "price")
    var price: Double? = null
}
