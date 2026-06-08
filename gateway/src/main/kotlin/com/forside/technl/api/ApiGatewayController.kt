package com.forside.technl.api

import com.forside.technl.generated.api.BeerApi
import com.forside.technl.generated.api.OrderApi
import com.forside.technl.generated.api.PaymentApi
import com.forside.technl.generated.model.*
import com.forside.technl.service.ServiceApi
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.*


@RestController
class ApiGatewayController (
    private val serviceApi: ServiceApi

): BeerApi, OrderApi, PaymentApi{

    companion object {
        private val log = LoggerFactory.getLogger(ApiGatewayController::class.java)
    }

    override fun getBeers(): ResponseEntity<List<BeerResponse>> {
        return ResponseEntity.status(HttpStatus.OK).body(
            serviceApi.getBeers()
        )
    }

    override fun createOrder(createOrderRequest: CreateOrderRequest): ResponseEntity<OrderResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            serviceApi.createOrder(createOrderRequest)
        )
    }

    override fun getOrders(): ResponseEntity<List<OrderResponse>> {
        return ResponseEntity.status(HttpStatus.OK).body(
            serviceApi.getOrders()
        )
    }

    override fun payOrder(orderId: UUID, payOrderRequest: PayOrderRequest): ResponseEntity<PaymentResponse> {
        return ResponseEntity.status(HttpStatus.OK).body(
            serviceApi.payOrder(payOrderRequest, orderId)
        )
    }
}