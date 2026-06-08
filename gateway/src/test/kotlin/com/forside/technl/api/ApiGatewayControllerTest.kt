package com.forside.technl.api

import com.forside.technl.generated.model.*
import com.forside.technl.service.ServiceApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import java.util.*

class ApiGatewayControllerTest {

    private val serviceApi: ServiceApi = mock(ServiceApi::class.java)
    private val controller = ApiGatewayController(serviceApi)

    @Test
    fun `getBeers should return OK and beers from service`() {
        val beers = listOf(
            BeerResponse(
                id = "1",
                name = "Heineken",
                price = 4.50
            ),
            BeerResponse(
                id = "2",
                name = "Corona",
                price = 5.00
            )
        )

        `when`(serviceApi.getBeers()).thenReturn(beers)

        val response = controller.getBeers()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertSame(beers, response.body)
        verify(serviceApi).getBeers()
    }

    @Test
    fun `createOrder should return CREATED and created order from service`() {
        val request = CreateOrderRequest(
            items = listOf(
                CreateOrderItemRequest(
                    beerId = "1",
                    quantity = 2
                )
            )
        )

        val createdOrder = OrderResponse(
            orderId = UUID.randomUUID(),
            status = OrderStatus.CREATED,
            totalAmount = 9.00,
            items = emptyList(),
            createdAt = null
        )

        `when`(serviceApi.createOrder(request)).thenReturn(createdOrder)

        val response = controller.createOrder(request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertSame(createdOrder, response.body)
        verify(serviceApi).createOrder(request)
    }

    @Test
    fun `getOrders should return OK and orders from service`() {
        val orders = listOf(
            OrderResponse(
                orderId = UUID.randomUUID(),
                status = OrderStatus.CREATED,
                totalAmount = 12.50,
                items = emptyList(),
                createdAt = null
            )
        )

        `when`(serviceApi.getOrders()).thenReturn(orders)

        val response = controller.getOrders()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertSame(orders, response.body)
        verify(serviceApi).getOrders()
    }

    @Test
    fun `payOrder should return OK and payment response from service`() {
        val orderId = UUID.randomUUID()
        val request = PayOrderRequest(
            paymentMethod = PaymentMethod.CARD,
            amount = 12.50
        )

        val paymentResponse = PaymentResponse(
            orderId = orderId,
            status = PaymentStatus.PAID
        )

        `when`(serviceApi.payOrder(request, orderId)).thenReturn(paymentResponse)

        val response = controller.payOrder(orderId, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertSame(paymentResponse, response.body)
        verify(serviceApi).payOrder(request, orderId)
    }
}