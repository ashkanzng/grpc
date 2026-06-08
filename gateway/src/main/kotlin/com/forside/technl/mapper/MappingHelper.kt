package com.forside.technl.mapper

import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class MappingHelper {
    fun map(value: String?): OffsetDateTime? =
        value?.let(OffsetDateTime::parse)
}