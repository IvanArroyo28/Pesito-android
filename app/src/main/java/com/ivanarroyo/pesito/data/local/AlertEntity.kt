package com.ivanarroyo.pesito.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromCode: String,
    val toCode: String,
    /** "ABOVE" o "BELOW". */
    val direction: String,
    /** Como texto, igual que las tasas, para no perder decimales. */
    val target: String,
    val isActive: Boolean,
    val createdAt: Long,
)
