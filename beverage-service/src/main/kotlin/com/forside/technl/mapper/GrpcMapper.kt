package com.forside.technl.mapper

import com.forside.technl.domain.BeerEntity
import com.forside.technl.domain.OrderEntity
import com.forside.technl.domain.OrderItemEntity
import com.forside.technl.grpc.Beer
import com.forside.technl.grpc.CreateOrderResponse
import com.forside.technl.grpc.OrderItemResponse
import com.forside.technl.grpc.PayOrderResponse
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
abstract class GrpcMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "price", source = "price")
    abstract fun toGrpcBeer(entity: BeerEntity): Beer

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "totalAmount", source = "totalAmount")
    abstract fun toCreateOrderResponse(entity: OrderEntity): CreateOrderResponse

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    abstract fun toPayOrderResponse(entity: OrderEntity): PayOrderResponse

    fun toGetOrdersResponse(
        entities: List<OrderEntity>
    ): List<com.forside.technl.grpc.Order> {
        return entities.map { toOrder(it) }
    }

    fun toOrder(entity: OrderEntity): com.forside.technl.grpc.Order {
        return com.forside.technl.grpc.Order.newBuilder()
            .setOrderId(entity.id.toString())
            .setTotalAmount(entity.totalAmount.toDouble())
            .setStatus(entity.status.name)
            .setPaymentMethod(entity.paymentMethod ?: "")
            .setPaidAt(map(entity.paidAt) ?: "")
            .setCreatedAt(map(entity.createdAt) ?: "")
            .addAllItems(entity.items.map { toOrderItemResponse(it) })
            .build()
    }
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "beerId", source = "beer.id")
    @Mapping(target = "beerName", source = "beer.name")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "totalPrice", source = "totalPrice")
    abstract fun toOrderItemResponse(entity: OrderItemEntity): OrderItemResponse

    fun map(value: OffsetDateTime?): String? =
        value?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}