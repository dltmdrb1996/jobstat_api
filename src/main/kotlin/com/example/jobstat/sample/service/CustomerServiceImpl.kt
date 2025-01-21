package com.example.jobstat.sample.service

import com.example.jobstat.sample.entity.Customer
import com.example.jobstat.sample.respository.CustomerRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
internal class CustomerServiceImpl(
    private val customerRepository: CustomerRepository, // Use the custom repository
) : CustomerService {
    override fun makeCustomer(
        name: String,
        income: BigDecimal,
    ): Customer {
        val customer = Customer(name = name, income = income)
        return customerRepository.save(customer)
    }

    override fun getCustomer(id: Int): Customer = customerRepository.findById(id)
}
