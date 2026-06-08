package com.forside.technl.service

import com.forside.technl.domain.BeerEntity
import com.forside.technl.domain.OrderEntity
import com.forside.technl.domain.OrderItemEntity
import com.forside.technl.domain.OrderStatusEntity
import com.forside.technl.grpc.CreateOrderRequest
import com.forside.technl.grpc.CreateOrderResponse
import com.forside.technl.grpc.GetBeersResponse
import com.forside.technl.grpc.GetOrdersResponse
import com.forside.technl.grpc.PayOrderRequest
import com.forside.technl.grpc.PayOrderResponse
import com.forside.technl.mapper.GrpcMapper
import com.forside.technl.repository.BeerRepository
import com.forside.technl.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Service
class OrderService(
    private val beerRepository: BeerRepository,
    private val orderRepository: OrderRepository,
    private val grpcMapper: GrpcMapper
) {

    @Transactional(readOnly = true)
    fun getBeers(): GetBeersResponse =
        GetBeersResponse.newBuilder()
            .addAllBeers(beerRepository.findAll().map(grpcMapper::toGrpcBeer))
            .build()

    @Transactional
    fun createOrder(request: CreateOrderRequest): CreateOrderResponse {
        validateOrderItems(request)

        val beerIds = request.itemsList.map { parseUuid(it.beerId, "beerId") }
        val beersById = beerRepository.findAllById(beerIds).associateBy { it.id }
        validateAllBeersExist(beerIds, beersById.keys)

        val now = OffsetDateTime.now()
        val order = createOrderEntity(now)
        val orderItems = buildOrderItems(request, order, beersById, now)

        order.items.addAll(orderItems)
        order.totalAmount = calculateTotalAmount(orderItems)

        return grpcMapper.toCreateOrderResponse(orderRepository.save(order))
    }

    @Transactional
    fun payOrder(request: PayOrderRequest): PayOrderResponse {
        val orderId = parseUuid(request.orderId, "orderId")
        val order = orderRepository.findById(orderId)
            .orElseThrow { IllegalArgumentException("Order not found: $orderId") }

        order.status = OrderStatusEntity.PAID
        order.paymentMethod = request.paymentMethod
        order.paidAt = OffsetDateTime.now()

        return grpcMapper.toPayOrderResponse(orderRepository.save(order))
    }

    @Transactional(readOnly = true)
    fun getOrders(): GetOrdersResponse =
        GetOrdersResponse.newBuilder()
            .addAllOrders(grpcMapper.toGetOrdersResponse(orderRepository.findAll()))
            .build()

    private fun validateOrderItems(request: CreateOrderRequest) {
        require(request.itemsList.isNotEmpty()) {
            "Order must contain at least one item"
        }
    }

    private fun validateAllBeersExist(
        requestedBeerIds: List<UUID>,
        foundBeerIds: Set<UUID?>
    ) {
        val missingBeerIds = requestedBeerIds.filterNot(foundBeerIds::contains)
        require(missingBeerIds.isEmpty()) {
            "Beer not found: $missingBeerIds"
        }
    }

    private fun createOrderEntity(now: OffsetDateTime) = OrderEntity(
        totalAmount = BigDecimal.ZERO,
        status = OrderStatusEntity.CREATED,
        createdAt = now
    )

    private fun buildOrderItems(
        request: CreateOrderRequest,
        order: OrderEntity,
        beersById: Map<UUID?, *>,
        now: OffsetDateTime
    ): List<OrderItemEntity> {
        return request.itemsList.map { item ->
            val beerId = parseUuid(item.beerId, "beerId")
            val beer = beersById[beerId] as? BeerEntity
                ?: throw IllegalArgumentException("Beer not found: $beerId")

            val totalPrice = beer.price.multiply(item.quantity.toBigDecimal())
            OrderItemEntity(
                order = order,
                beer = beer,
                quantity = item.quantity,
                unitPrice = beer.price,
                totalPrice = totalPrice,
                createdAt = now
            )
        }
    }

    private fun calculateTotalAmount(orderItems: List<OrderItemEntity>): BigDecimal =
        orderItems.fold(BigDecimal.ZERO) { total, item -> total + item.totalPrice }

    private fun parseUuid(value: String, fieldName: String): UUID =
        try {
            UUID.fromString(value)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid $fieldName: $value")
        }
}