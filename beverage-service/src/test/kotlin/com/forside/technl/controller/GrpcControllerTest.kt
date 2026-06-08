package com.forside.technl.controller

import com.forside.technl.grpc.*
import com.forside.technl.service.OrderService
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GrpcControllerTest {

    @Mock
    lateinit var orderService: OrderService

    private lateinit var controller: GrpcController

    @BeforeEach
    fun setUp() {
        controller = GrpcController(orderService)
    }

    @Test
    fun `getBeers should return response and complete`() {
        val request = GetBeersRequest.newBuilder().build()
        val response = GetBeersResponse.newBuilder().build()

        @Suppress("UNCHECKED_CAST")
        val responseObserver = org.mockito.Mockito.mock(StreamObserver::class.java) as StreamObserver<GetBeersResponse>

        `when`(orderService.getBeers()).thenReturn(response)

        controller.getBeers(request, responseObserver)

        verify(orderService).getBeers()
        verify(responseObserver).onNext(response)
        verify(responseObserver).onCompleted()
        verify(responseObserver, never()).onError(any())
    }

    @Test
    fun `getBeers should send error when service throws`() {
        val request = GetBeersRequest.newBuilder().build()
        val exception = IllegalArgumentException("boom")

        @Suppress("UNCHECKED_CAST")
        val responseObserver = org.mockito.Mockito.mock(StreamObserver::class.java) as StreamObserver<GetBeersResponse>

        `when`(orderService.getBeers()).thenThrow(exception)

        controller.getBeers(request, responseObserver)

        verify(orderService).getBeers()
        verify(responseObserver).onError(exception)
        verify(responseObserver, never()).onNext(any())
        verify(responseObserver, never()).onCompleted()
    }

    @Test
    fun `createOrder should return response and complete`() {
        val request = CreateOrderRequest.newBuilder().build()
        val response = CreateOrderResponse.newBuilder()
            .setOrderId("order-1")
            .setStatus("CREATED")
            .setTotalAmount(12.5)
            .build()

        @Suppress("UNCHECKED_CAST")
        val responseObserver = org.mockito.Mockito.mock(StreamObserver::class.java) as StreamObserver<CreateOrderResponse>

        `when`(orderService.createOrder(request)).thenReturn(response)

        controller.createOrder(request, responseObserver)

        verify(orderService).createOrder(request)
        verify(responseObserver).onNext(response)
        verify(responseObserver).onCompleted()
        verify(responseObserver, never()).onError(any())
    }

    @Test
    fun `createOrder should send error when service throws`() {
        val request = CreateOrderRequest.newBuilder().build()
        val exception = IllegalArgumentException("invalid order")

        @Suppress("UNCHECKED_CAST")
        val responseObserver = org.mockito.Mockito.mock(StreamObserver::class.java) as StreamObserver<CreateOrderResponse>

        `when`(orderService.createOrder(request)).thenThrow(exception)

        controller.createOrder(request, responseObserver)

        verify(orderService).createOrder(request)
        verify(responseObserver).onError(exception)
        verify(responseObserver, never()).onNext(any())
        verify(responseObserver, never()).onCompleted()
    }

    @Test
    fun `payOrder should return response and complete`() {
        val request = PayOrderRequest.newBuilder()
            .setOrderId("order-1")
            .setPaymentMethod("CARD")
            .setAmount(12.5)
            .build()

        val response = PayOrderResponse.newBuilder()
            .setOrderId("order-1")
            .setStatus("PAID")
            .build()

        @Suppress("UNCHECKED_CAST")
        val responseObserver = org.mockito.Mockito.mock(StreamObserver::class.java) as StreamObserver<PayOrderResponse>

        `when`(orderService.payOrder(request)).thenReturn(response)

        controller.payOrder(request, responseObserver)

        verify(orderService).payOrder(request)
        verify(responseObserver).onNext(response)
        verify(responseObserver).onCompleted()
        verify(responseObserver, never()).onError(any())
    }

    @Test
    fun `payOrder should send error when service throws`() {
        val request = PayOrderRequest.newBuilder()
            .setOrderId("missing-order")
            .setPaymentMethod("CARD")
            .setAmount(12.5)
            .build()

        val exception = IllegalArgumentException("Order not found")

        @Suppress("UNCHECKED_CAST")
        val responseObserver = org.mockito.Mockito.mock(StreamObserver::class.java) as StreamObserver<PayOrderResponse>

        `when`(orderService.payOrder(request)).thenThrow(exception)

        controller.payOrder(request, responseObserver)

        verify(orderService).payOrder(request)
        verify(responseObserver).onError(exception)
        verify(responseObserver, never()).onNext(any())
        verify(responseObserver, never()).onCompleted()
    }

    @Test
    fun `getOrders should return response and complete`() {
        val request = GetOrdersRequest.newBuilder().build()
        val response = GetOrdersResponse.newBuilder().build()

        @Suppress("UNCHECKED_CAST")
        val responseObserver = org.mockito.Mockito.mock(StreamObserver::class.java) as StreamObserver<GetOrdersResponse>

        `when`(orderService.getOrders()).thenReturn(response)

        controller.getOrders(request, responseObserver)

        verify(orderService).getOrders()
        verify(responseObserver).onNext(response)
        verify(responseObserver).onCompleted()
        verify(responseObserver, never()).onError(any())
    }

    @Test
    fun `getOrders should send error when service throws`() {
        val request = GetOrdersRequest.newBuilder().build()
        val exception = RuntimeException("unexpected")

        @Suppress("UNCHECKED_CAST")
        val responseObserver = org.mockito.Mockito.mock(StreamObserver::class.java) as StreamObserver<GetOrdersResponse>

        `when`(orderService.getOrders()).thenThrow(exception)

        controller.getOrders(request, responseObserver)

        verify(orderService).getOrders()
        verify(responseObserver).onError(exception)
        verify(responseObserver, never()).onNext(any())
        verify(responseObserver, never()).onCompleted()
    }
}