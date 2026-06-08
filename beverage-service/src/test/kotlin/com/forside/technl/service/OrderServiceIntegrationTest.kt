package com.forside.technl.service

import com.forside.technl.SandboxApplication
import com.forside.technl.domain.OrderStatusEntity
import com.forside.technl.grpc.CreateOrderRequest
import com.forside.technl.grpc.GetOrdersResponse
import com.forside.technl.grpc.OrderItem
import com.forside.technl.grpc.PayOrderRequest
import com.forside.technl.repository.BeerRepository
import com.forside.technl.repository.OrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@SpringBootTest(classes = [SandboxApplication::class])
@ActiveProfiles("test")
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    lateinit var orderService: OrderService

    @Autowired
    lateinit var beerRepository: BeerRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    private lateinit var heinekenId: UUID
    private lateinit var coronaId: UUID

    @BeforeEach
    fun setUp() {
        heinekenId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        coronaId = UUID.fromString("22222222-2222-2222-2222-222222222222")
    }

    @Test
    fun `getBeers returns seeded beers from database`() {
        val response = orderService.getBeers()

        assertThat(response.beersList).hasSize(4)
        assertThat(response.beersList.map { it.name })
            .contains("Heineken", "Corona", "Amstel", "Bavaria")

        val heineken = response.beersList.first { it.id == heinekenId.toString() }
        assertThat(heineken.name).isEqualTo("Heineken")
        assertThat(heineken.price).isEqualTo(3.5)
    }

    @Test
    fun `createOrder persists order and items and calculates total amount`() {
        val request = CreateOrderRequest.newBuilder()
            .addItems(
                OrderItem.newBuilder()
                    .setBeerId(heinekenId.toString())
                    .setQuantity(2)
                    .build()
            )
            .addItems(
                OrderItem.newBuilder()
                    .setBeerId(coronaId.toString())
                    .setQuantity(3)
                    .build()
            )
            .build()

        val response = orderService.createOrder(request)

        assertThat(response.orderId).isNotBlank()
        assertThat(response.status).isEqualTo("CREATED")
        assertThat(response.totalAmount).isEqualTo(19.0) // 2*3.5 + 3*4.0

        val savedOrder = orderRepository.findById(UUID.fromString(response.orderId)).orElseThrow()

        assertThat(savedOrder.status).isEqualTo(OrderStatusEntity.CREATED)
        assertThat(savedOrder.paymentMethod).isNull()
        assertThat(savedOrder.paidAt).isNull()
        assertThat(savedOrder.totalAmount).isEqualByComparingTo(BigDecimal("19.00"))
        assertThat(savedOrder.items).hasSize(2)

        val heinekenItem = savedOrder.items.first { it.beer.id == heinekenId }
        assertThat(heinekenItem.quantity).isEqualTo(2)
        assertThat(heinekenItem.unitPrice).isEqualByComparingTo(BigDecimal("3.50"))
        assertThat(heinekenItem.totalPrice).isEqualByComparingTo(BigDecimal("7.00"))

        val coronaItem = savedOrder.items.first { it.beer.id == coronaId }
        assertThat(coronaItem.quantity).isEqualTo(3)
        assertThat(coronaItem.unitPrice).isEqualByComparingTo(BigDecimal("4.00"))
        assertThat(coronaItem.totalPrice).isEqualByComparingTo(BigDecimal("12.00"))
    }

    @Test
    fun `createOrder throws when request has no items`() {
        val request = CreateOrderRequest.newBuilder().build()

        assertThatThrownBy { orderService.createOrder(request) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Order must contain at least one item")
    }

    @Test
    fun `createOrder throws when beer does not exist`() {
        val missingBeerId = UUID.randomUUID()

        val request = CreateOrderRequest.newBuilder()
            .addItems(
                OrderItem.newBuilder()
                    .setBeerId(missingBeerId.toString())
                    .setQuantity(1)
                    .build()
            )
            .build()

        assertThatThrownBy { orderService.createOrder(request) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Beer not found")
            .hasMessageContaining(missingBeerId.toString())
    }

    @Test
    fun `payOrder updates order status payment method and paid timestamp`() {
        val createResponse = orderService.createOrder(
            CreateOrderRequest.newBuilder()
                .addItems(
                    OrderItem.newBuilder()
                        .setBeerId(heinekenId.toString())
                        .setQuantity(2)
                        .build()
                )
                .build()
        )

        val payResponse = orderService.payOrder(
            PayOrderRequest.newBuilder()
                .setOrderId(createResponse.orderId)
                .setPaymentMethod("CREDIT_CARD")
                .setAmount(createResponse.totalAmount)
                .build()
        )

        assertThat(payResponse.orderId).isEqualTo(createResponse.orderId)
        assertThat(payResponse.status).isEqualTo("PAID")

        val paidOrder = orderRepository.findById(UUID.fromString(createResponse.orderId)).orElseThrow()
        assertThat(paidOrder.status).isEqualTo(OrderStatusEntity.PAID)
        assertThat(paidOrder.paymentMethod).isEqualTo("CREDIT_CARD")
        assertThat(paidOrder.paidAt).isNotNull()
    }

    @Test
    fun `payOrder throws when order does not exist`() {
        val missingOrderId = UUID.randomUUID()

        val request = PayOrderRequest.newBuilder()
            .setOrderId(missingOrderId.toString())
            .setPaymentMethod("CASH")
            .setAmount(10.0)
            .build()

        assertThatThrownBy { orderService.payOrder(request) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Order not found")
            .hasMessageContaining(missingOrderId.toString())
    }

    @Test
    fun `getOrders returns created orders with items`() {
        val createResponse = orderService.createOrder(
            CreateOrderRequest.newBuilder()
                .addItems(
                    OrderItem.newBuilder()
                        .setBeerId(heinekenId.toString())
                        .setQuantity(1)
                        .build()
                )
                .addItems(
                    OrderItem.newBuilder()
                        .setBeerId(coronaId.toString())
                        .setQuantity(2)
                        .build()
                )
                .build()
        )

        val response: GetOrdersResponse = orderService.getOrders()

        assertThat(response.ordersList).hasSize(1)

        val order = response.ordersList.first()
        assertThat(order.orderId).isEqualTo(createResponse.orderId)
        assertThat(order.status).isEqualTo("CREATED")
        assertThat(order.paymentMethod).isEmpty()
        assertThat(order.createdAt).isNotBlank()
        assertThat(order.itemsList).hasSize(2)

        val itemBeerIds = order.itemsList.map { it.beerId }
        assertThat(itemBeerIds).contains(heinekenId.toString(), coronaId.toString())
    }
}