package com.example.jobstat.sample

import ApiResponse
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.sample.usecase.single.GetCustomer
import com.example.jobstat.sample.usecase.single.MakeCustomer
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/customers") // Add the '/api' prefix
internal class CustomerController(
    private val getCustomer: GetCustomer,
    private val makeCustomer: MakeCustomer,
) {
    @PostMapping
    fun postCustomer(
        @RequestBody request: MakeCustomer.Request,
    ): ResponseEntity<ApiResponse<MakeCustomer.Response>> {
        val responseDto = makeCustomer(request)
        return ApiResponse.ok(responseDto)
    }

    @GetMapping("/{id}")
    fun getCustomerById(
        @PathVariable id: Int,
    ): ResponseEntity<ApiResponse<GetCustomer.Response>> {
        val responseDto = getCustomer(GetCustomer.Request(id, "test${LocalDate.now()}"))
        return ApiResponse.ok(responseDto)
    }
}
