package com.ivanarroyo.pesito.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v2 → v3: se agrega la tabla de alertas.
 *
 * Hasta la Fase 6 la base de datos era solo una copia de la API y podíamos borrarla.
 * Ahora guarda ALERTAS DEL USUARIO, así que escribimos una migración de verdad:
 * crea la tabla nueva sin tocar las que ya existen.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `alerts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `fromCode` TEXT NOT NULL,
                `toCode` TEXT NOT NULL,
                `direction` TEXT NOT NULL,
                `target` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}
