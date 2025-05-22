package com.wildrew.app.statistics_read.core.core_model

import java.time.LocalDate

data class JobListing(
    val id: String,
    val url: String,
    val registrationDate: LocalDate,
)
