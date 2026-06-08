package com.forside.technl.mapper

import com.forside.technl.generated.model.*
import com.forside.technl.generated.model.OrderItemResponse
import com.forside.technl.grpc.*
import com.forside.technl.grpc.CreateOrderRequest
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import java.util.*

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    uses = [MappingHelper::class]
)
abstract class GrpcMapper {

    abstract fun toBeerResponse(beer: Beer): BeerResponse
    abstract fun toBeerResponses(beers: List<Beer>): List<BeerResponse>

    fun toCreateOrderRequest(
        orderRequest: com.forside.technl.generated.model.CreateOrderRequest
    ): CreateOrderRequest {
        return CreateOrderRequest.newBuilder()
            .addAllItems(orderRequest.items.map { toGrpcOrderItem(it) })
            .build()
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "beerId", source = "beerId")
    @Mapping(target = "quantity", source = "quantity")
    abstract fun toGrpcOrderItem(item: CreateOrderItemRequest): OrderItem

    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "items", ignore = true)
    @BeanMapping(ignoreByDefault = true)
    abstract fun toCreateOrderResponse(
        orderResponse: CreateOrderResponse
    ): OrderResponse

    fun toGetOrdersResponse(response: List<Order>): List<OrderResponse> {
        return response.map { toOrderResponse(it) }
    }

    fun toOrderResponse(order: Order): OrderResponse {
        return OrderResponse(
            orderId = UUID.fromString(order.orderId),
            status = OrderStatus.valueOf(order.status),
            totalAmount = order.totalAmount,
            items = order.itemsList.map { item ->
                OrderItemResponse(
                    beerId = item.beerId,
                    beerName = item.beerName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    totalPrice = item.totalPrice
                )
            },
            createdAt = order.createdAt?.let(java.time.OffsetDateTime::parse)
        )
    }

    fun toPaymentResponse(response: PayOrderResponse): PaymentResponse {
        return PaymentResponse(
            orderId = UUID.fromString(response.orderId),
            status = PaymentStatus.valueOf(response.status)
        )
    }
}