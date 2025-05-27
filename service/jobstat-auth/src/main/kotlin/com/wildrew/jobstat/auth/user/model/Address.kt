package com.wildrew.jobstat.auth.user.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Address(
    @Column(length = 100)
    val do_si: String,
    @Column(length = 50)
    val address: String,
    @Column(length = 10)
    val zipCode: String,
)
