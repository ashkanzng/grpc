package com.forside.technl.service

import com.forside.technl.domain.OrderEntity
import com.forside.technl.domain.OrderItemEntity
import com.forside.technl.domain.OrderStatusEntity
import com.forside.technl.grpc.*
import com.forside.technl.mapper.GrpcMapper
import com.forside.technl.repository.BeerRepository
import com.forside.technl.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@Service
class OrderService(
    private val beerRepository: BeerRepository,
    private val orderRepository: OrderRepository,
    private val grpcMapper: GrpcMapper
) {

    @Transactional(readOnly = true)
    fun getBeers(): GetBeersResponse {
        val beers = beerRepository.findAll()
        return GetBeersResponse.newBuilder()
            .addAllBeers(beers.map { grpcMapper.toGrpcBeer(it) })
            .build()
    }

    @Transactional
    fun createOrder(request: CreateOrderRequest): CreateOrderResponse {
        if (request.itemsList.isEmpty()) {
            throw IllegalArgumentException("Order must contain at least one item")
        }

        val beerIds = request.itemsList.map { UUID.fromString(it.beerId) }
        val beersById = beerRepository.findAllById(beerIds)
            .associateBy { it.id }
        val missingBeerIds = beerIds.filterNot { beersById.containsKey(it) }
        if (missingBeerIds.isNotEmpty()) {
            throw IllegalArgumentException("Beer not found: $missingBeerIds")
        }

        val now = OffsetDateTime.now()
        val order = OrderEntity(
            totalAmount = BigDecimal.ZERO,
            status = OrderStatusEntity.CREATED,
            createdAt = now
        )

        val orderItems = request.itemsList.map { item ->
            val beerId = UUID.fromString(item.beerId)
            val beer = beersById[beerId] ?: throw IllegalArgumentException("Beer not found: $beerId")
            val unitPrice = beer.price
            val totalPrice = unitPrice.multiply(item.quantity.toBigDecimal())

            OrderItemEntity(
                order = order,
                beer = beer,
                quantity = item.quantity,
                unitPrice = unitPrice,
                totalPrice = totalPrice,
                createdAt = now
            )
        }

        val totalAmount = orderItems
            .map { it.totalPrice }
            .fold(BigDecimal.ZERO) { acc, price -> acc + price }

        order.totalAmount = totalAmount
        order.items.addAll(orderItems)
        val savedOrder = orderRepository.save(order)

        return grpcMapper.toCreateOrderResponse(savedOrder)
    }

    @Transactional
    fun payOrder(request: PayOrderRequest): PayOrderResponse {
        val orderId = UUID.fromString(request.orderId)
        val order = orderRepository.findById(orderId)
            .orElseThrow { IllegalArgumentException("Order not found: $orderId") }
        order.status = OrderStatusEntity.PAID
        order.paymentMethod = request.paymentMethod
        order.paidAt = OffsetDateTime.now()
        val savedOrder = orderRepository.save(order)

        return grpcMapper.toPayOrderResponse(savedOrder)
    }

    @Transactional(readOnly = true)
    fun getOrders(): GetOrdersResponse {
        val orders = orderRepository.findAll()

        return GetOrdersResponse.newBuilder()
            .addAllOrders(grpcMapper.toGetOrdersResponse(orders))
            .build()
    }
}