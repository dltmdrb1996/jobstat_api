package com.example.jobstat.core.state

import java.time.LocalDate

data class JobListing(
    val id: String,
    val url: String,
    val registrationDate: LocalDate,
)
