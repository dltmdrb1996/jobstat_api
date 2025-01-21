package com.example.jobstat.sample.respository

import com.example.jobstat.sample.entity.Customer

internal interface CustomerRepository {
    fun findById(id: Int): Customer

    fun save(customer: Customer): Customer
}
