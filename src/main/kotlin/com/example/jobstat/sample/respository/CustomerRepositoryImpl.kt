package com.example.jobstat.sample.respository

import com.example.jobstat.core.extension.orThrowNotFound
import com.example.jobstat.sample.entity.Customer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

internal interface CustomerJpaRepository : JpaRepository<Customer, Int>

@Repository
internal class CustomerRepositoryImpl(
    private val customerJpaRepository: CustomerJpaRepository,
) : CustomerRepository {
    override fun save(customer: Customer): Customer = customerJpaRepository.save(customer)

    override fun findById(id: Int): Customer = customerJpaRepository.findById(id).orThrowNotFound("Customer", id)
}
