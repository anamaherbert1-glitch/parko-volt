package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        VehicleEntity::class,
        ParkingLocationEntity::class,
        AttendantEntity::class,
        ParkingSessionEntity::class,
        PaymentRecordEntity::class,
        ManagerApplicationEntity::class,
        IncidentEntity::class,
        NotificationEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ParkoDatabase : RoomDatabase() {
    abstract fun parkoDao(): ParkoDao

    companion object {
        @Volatile
        private var INSTANCE: ParkoDatabase? = null

        fun getInstance(context: Context): ParkoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ParkoDatabase::class.java,
                    "parko_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
