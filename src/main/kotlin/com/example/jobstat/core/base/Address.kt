package com.example.jobstat.core.base

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Address(
    @Column(length = 100)
    val street: String,
    @Column(length = 50)
    val city: String,
    @Column(length = 10)
    val zipCode: String,
)
