package com.forside.technl.repository

import com.forside.technl.domain.BeerEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BeerRepository : JpaRepository<BeerEntity, UUID> {
    fun findByName(name: String): BeerEntity?
}