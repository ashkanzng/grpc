package com.forside.technl.repository

import com.forside.technl.domain.OrderEntity
import com.forside.technl.domain.OrderStatusEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OrderRepository : JpaRepository<OrderEntity, UUID> {

    fun findAllByStatus(status: OrderStatusEntity): List<OrderEntity>

}