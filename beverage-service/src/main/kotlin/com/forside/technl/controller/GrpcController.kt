package com.forside.technl.controller

import com.forside.technl.grpc.*
import com.forside.technl.service.OrderService
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class GrpcController(
    private val service: OrderService
) : OrderGrpcServiceGrpc.OrderGrpcServiceImplBase() {

    override fun getBeers(
        request: GetBeersRequest,
        responseObserver: StreamObserver<GetBeersResponse>
    ) = respond(responseObserver) {
        service.getBeers()
    }

    override fun createOrder(
        request: CreateOrderRequest,
        responseObserver: StreamObserver<CreateOrderResponse>
    ) = respond(responseObserver) {
        service.createOrder(request)
    }

    override fun payOrder(
        request: PayOrderRequest,
        responseObserver: StreamObserver<PayOrderResponse>
    ) = respond(responseObserver) {
        service.payOrder(request)
    }

    override fun getOrders(
        request: GetOrdersRequest,
        responseObserver: StreamObserver<GetOrdersResponse>
    ) = respond(responseObserver) {
        service.getOrders()
    }

    private fun <T> respond(
        responseObserver: StreamObserver<T>,
        supplier: () -> T
    ) {
        try {
            responseObserver.onNext(supplier())
            responseObserver.onCompleted()
        } catch (ex: Exception) {
            responseObserver.onError(ex)
        }
    }
}