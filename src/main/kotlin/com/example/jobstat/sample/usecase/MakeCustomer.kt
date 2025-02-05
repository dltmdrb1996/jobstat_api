package com.example.jobstat.sample.usecase

import com.example.jobstat.core.usecase.impl.BaseUseCase
import com.example.jobstat.sample.entity.Customer
import com.example.jobstat.sample.service.CustomerService
import jakarta.validation.constraints.NotEmpty
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
internal class MakeCustomer(
    private val customerService: CustomerService,
) : BaseUseCase<MakeCustomer.Request, MakeCustomer.Response>() {
    override fun execute(request: Request): Response = customerService.makeCustomer(request.name, request.income).toResponse()

    data class Request(
        @NotEmpty val name: String,
        @NotEmpty val income: BigDecimal,
    )

    data class Response(
        @NotEmpty val id: Long,
        @NotEmpty val name: String,
        @NotEmpty val income: Int,
    )

    fun Customer.toResponse(): Response =
        Response(
            id = this.id,
            name = this.name,
            income = this.income.toInt(),
        )
}
