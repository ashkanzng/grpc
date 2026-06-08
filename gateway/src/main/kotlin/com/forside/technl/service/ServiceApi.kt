package com.forside.technl.service

import com.forside.technl.generated.model.BeerResponse
import com.forside.technl.generated.model.CreateOrderRequest
import com.forside.technl.generated.model.OrderResponse
import com.forside.technl.generated.model.PaymentResponse
import com.forside.technl.grpc.GetBeersRequest
import com.forside.technl.grpc.GetOrdersRequest
import com.forside.technl.grpc.OrderGrpcServiceGrpc
import com.forside.technl.grpc.PayOrderRequest
import com.forside.technl.mapper.GrpcMapper
import net.devh.boot.grpc.client.inject.GrpcClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*


@Service
class ServiceApi(
    private val grpcMapper: GrpcMapper
){
    companion object {
        private val log = LoggerFactory.getLogger(ServiceApi::class.java)
    }

    @GrpcClient("order-service")
    private lateinit var orderStub: OrderGrpcServiceGrpc.OrderGrpcServiceBlockingStub

    fun getBeers(): List<BeerResponse> {
        val response = grpcMapper.toBeerResponses(
            orderStub.getBeers(GetBeersRequest.newBuilder().build()).beersList)
        return response
    }

    fun createOrder(request: CreateOrderRequest): OrderResponse {
        val request = grpcMapper.toCreateOrderRequest(request)
        val response = orderStub.createOrder(request)
        return grpcMapper.toCreateOrderResponse(response)
    }

    fun getOrders(): List<OrderResponse> {
        val response = orderStub.getOrders(GetOrdersRequest.newBuilder().build())
        return grpcMapper.toGetOrdersResponse(response.ordersList)
    }

    fun payOrder(request: com.forside.technl.generated.model.PayOrderRequest, id: UUID): PaymentResponse {
        val response = orderStub.payOrder(
            PayOrderRequest.newBuilder()
                .setOrderId(id.toString())
                .setPaymentMethod(request.paymentMethod.name)
                .build()
        )
        return grpcMapper.toPaymentResponse(response)
    }

}