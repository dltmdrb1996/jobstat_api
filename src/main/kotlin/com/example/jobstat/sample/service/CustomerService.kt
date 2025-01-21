package com.example.jobstat.sample.service

import com.example.jobstat.sample.entity.Customer
import java.math.BigDecimal

interface CustomerService {
    fun makeCustomer(
        name: String,
        income: BigDecimal,
    ): Customer

    fun getCustomer(id: Int): Customer
}
