package com.example.jobstat.sample.usecase

import com.example.jobstat.core.usecase.LoggedUseCase
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.sample.entity.Customer
import com.example.jobstat.sample.service.CustomerService
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@LoggedUseCase
internal class GetCustomer(
    private val customerService: CustomerService,
    validator: Validator,
) : ValidUseCase<GetCustomer.Request, GetCustomer.Response>(validator) {
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response {
        val customer = customerService.getCustomer(request.id)
        return customer.toCustomerResponse()
    }

    data class Request(
        @field:Min(value = 1, message = "ID must be positive")
        val id: Int,
        @field:NotEmpty(message = "Name must not be empty")
        val name: String,
    )

    data class Response(
        val id: Long,
        val name: String,
        val income: Int,
    )

    private fun Customer.toCustomerResponse(): Response =
        Response(
            id = this.id,
            name = this.name,
            income = this.income.toInt(),
        )
}
