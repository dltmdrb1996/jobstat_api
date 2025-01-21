package com.example.jobstat.sample.entity

import com.example.jobstat.core.base.BaseEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
class Customer(
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val income: BigDecimal,
) : BaseEntity()
