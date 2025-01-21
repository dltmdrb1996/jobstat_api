package com.example.jobstat.user.entity

import com.example.jobstat.core.base.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "orders")
internal class Order private constructor(
    @Column(nullable = false)
    val orderNumber: String,
) : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null
        internal set

    companion object {
        fun create(orderNumber: String): Order {
            require(orderNumber.isNotBlank()) { "Order number must not be blank" }
            return Order(orderNumber)
        }
    }
}
