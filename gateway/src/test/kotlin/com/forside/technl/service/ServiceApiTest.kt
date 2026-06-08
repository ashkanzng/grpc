package com.forside.technl.service

import com.forside.technl.generated.model.*
import com.forside.technl.generated.model.CreateOrderRequest
import com.forside.technl.grpc.*
import com.forside.technl.mapper.GrpcMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import java.lang.reflect.Field
import java.util.*
import com.forside.technl.generated.model.PayOrderRequest as ApiPayOrderRequest

class ServiceApiTest {

    private val grpcMapper: GrpcMapper = mock(GrpcMapper::class.java)
    private val orderStub: OrderGrpcServiceGrpc.OrderGrpcServiceBlockingStub =
        mock(OrderGrpcServiceGrpc.OrderGrpcServiceBlockingStub::class.java)

    private val serviceApi = ServiceApi(grpcMapper).also {
        setPrivateField(it, "orderStub", orderStub)
    }

    @Test
    fun `getBeers should call grpc stub and map response`() {
        val beersList = listOf(
            Beer.newBuilder()
                .setId("1")
                .setName("Heineken")
                .setPrice(4.5)
                .build(),
            Beer.newBuilder()
                .setId("2")
                .setName("Corona")
                .setPrice(5.0)
                .build()
        )

        val grpcResponse = GetBeersResponse.newBuilder()
            .addAllBeers(beersList)
            .build()

        val mappedResponse = listOf(
            BeerResponse(id = "1", name = "Heineken", price = 4.5),
            BeerResponse(id = "2", name = "Corona", price = 5.0)
        )

        `when`(orderStub.getBeers(any())).thenReturn(grpcResponse)
        `when`(grpcMapper.toBeerResponses(beersList)).thenReturn(mappedResponse)

        val result = serviceApi.getBeers()

        assertSame(mappedResponse, result)
        verify(orderStub).getBeers(any())
        verify(grpcMapper).toBeerResponses(beersList)
    }

    @Test
    fun `createOrder should map request call grpc stub and map response`() {
        val apiRequest = CreateOrderRequest(
            items = listOf(
                CreateOrderItemRequest(
                    beerId = "1",
                    quantity = 2
                )
            )
        )

        val grpcRequest = com.forside.technl.grpc.CreateOrderRequest.newBuilder()
            .addItems(
                com.forside.technl.grpc.OrderItem.newBuilder()
                    .setBeerId("1")
                    .setQuantity(2)
                    .build()
            )
            .build()

        val grpcResponse = CreateOrderResponse.newBuilder()
            .setOrderId(UUID.randomUUID().toString())
            .setStatus("CREATED")
            .setTotalAmount(9.0)
            .build()

        val mappedResponse = OrderResponse(
            orderId = UUID.fromString(grpcResponse.orderId),
            status = OrderStatus.CREATED,
            totalAmount = 9.0,
            createdAt = null
        )

        `when`(grpcMapper.toCreateOrderRequest(apiRequest)).thenReturn(grpcRequest)
        `when`(orderStub.createOrder(grpcRequest)).thenReturn(grpcResponse)
        `when`(grpcMapper.toCreateOrderResponse(grpcResponse)).thenReturn(mappedResponse)

        val result = serviceApi.createOrder(apiRequest)

        assertSame(mappedResponse, result)
        verify(grpcMapper).toCreateOrderRequest(apiRequest)
        verify(orderStub).createOrder(grpcRequest)
        verify(grpcMapper).toCreateOrderResponse(grpcResponse)
    }

    @Test
    fun `getOrders should call grpc stub and map orders`() {
        val grpcOrders = listOf(
            Order.newBuilder()
                .setOrderId(UUID.randomUUID().toString())
                .setStatus("CREATED")
                .setTotalAmount(12.5)
                .setCreatedAt("2024-05-21T12:00:00Z")
                .build()
        )

        val grpcResponse = GetOrdersResponse.newBuilder()
            .addAllOrders(grpcOrders)
            .build()

        val mappedResponse = listOf(
            OrderResponse(
                orderId = UUID.fromString(grpcOrders[0].orderId),
                status = OrderStatus.CREATED,
                totalAmount = 12.5,
                createdAt = java.time.OffsetDateTime.parse("2024-05-21T12:00:00Z")
            )
        )

        `when`(orderStub.getOrders(any())).thenReturn(grpcResponse)
        `when`(grpcMapper.toGetOrdersResponse(grpcOrders)).thenReturn(mappedResponse)

        val result = serviceApi.getOrders()

        assertSame(mappedResponse, result)
        verify(orderStub).getOrders(any())
        verify(grpcMapper).toGetOrdersResponse(grpcOrders)
    }

    @Test
    fun `payOrder should build grpc request call stub and map response`() {
        val orderId = UUID.randomUUID()
        val apiRequest = ApiPayOrderRequest(
            paymentMethod = PaymentMethod.CARD,
            amount = 12.5
        )

        val grpcResponse = PayOrderResponse.newBuilder()
            .setOrderId(orderId.toString())
            .setStatus("PAID")
            .build()

        val mappedResponse = PaymentResponse(
            orderId = orderId,
            status = PaymentStatus.PAID
        )

        `when`(orderStub.payOrder(any())).thenReturn(grpcResponse)
        `when`(grpcMapper.toPaymentResponse(grpcResponse)).thenReturn(mappedResponse)

        val result = serviceApi.payOrder(apiRequest, orderId)

        assertSame(mappedResponse, result)

        val captor = ArgumentCaptor.forClass(com.forside.technl.grpc.PayOrderRequest::class.java)
        verify(orderStub).payOrder(captor.capture())

        val sentRequest = captor.value
        assertEquals(orderId.toString(), sentRequest.orderId)
        assertEquals("CARD", sentRequest.paymentMethod)

        verify(grpcMapper).toPaymentResponse(grpcResponse)
    }

    private fun setPrivateField(target: Any, fieldName: String, value: Any) {
        val field: Field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}