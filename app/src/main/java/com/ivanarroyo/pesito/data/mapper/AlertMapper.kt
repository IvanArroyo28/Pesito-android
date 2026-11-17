package com.ivanarroyo.pesito.data.mapper

import com.ivanarroyo.pesito.data.local.AlertEntity
import com.ivanarroyo.pesito.domain.model.AlertDirection
import com.ivanarroyo.pesito.domain.model.RateAlert
import java.math.BigDecimal

fun AlertEntity.toDomain(): RateAlert = RateAlert(
    id = id,
    from = fromCode,
    to = toCode,
    direction = AlertDirection.valueOf(direction),
    target = BigDecimal(target),
    isActive = isActive,
)

fun RateAlert.toEntity(createdAt: Long): AlertEntity = AlertEntity(
    id = id,
    fromCode = from,
    toCode = to,
    direction = direction.name,
    target = target.toPlainString(),
    isActive = isActive,
    createdAt = createdAt,
)
