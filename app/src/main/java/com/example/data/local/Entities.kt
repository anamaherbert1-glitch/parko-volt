package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val role: String,
    val isAccountVerified: Boolean = true,
    val isSuspended: Boolean = false,
    val city: String = "Lomé",
    val neighborhood: String = "Tokoin",
    val preferredLanguage: String = "Français",
    val notificationsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val ownerName: String,
    val ownerPhone: String,
    val type: String,
    val plateNumber: String,
    val brand: String,
    val model: String,
    val color: String,
    val photoUri: String? = null,
    val drawableKey: String = "car",
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String get() = "$brand $model".trim().ifBlank { type }
}

@Entity(tableName = "parking_locations")
data class ParkingLocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val city: String,
    val neighborhood: String,
    val parkingType: String,
    val acceptedTypes: String,
    val managerUserId: String,
    val subscriptionTier: String,
    val subscriptionActive: Boolean,
    val subscriptionExpiresAt: Long,
    val quotaUsed: Int,
    val quotaLimit: Int,
    val isSuspended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendants")
data class AttendantEntity(
    @PrimaryKey val id: String,
    val parkingId: String,
    val userId: String,
    val fullName: String,
    val phone: String,
    val role: String,
    val isActive: Boolean = true,
    val registrationsCount: Int = 0,
    val scansCount: Int = 0,
    val exitsCount: Int = 0,
    val incidentsCount: Int = 0,
    val procedureScore: Int = 96,
    val badgeTier: String = "ARGENT",
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "parking_sessions")
data class ParkingSessionEntity(
    @PrimaryKey val id: String,
    val referenceCode: String,
    val qrToken: String,
    val parkingId: String,
    val parkingName: String,
    val vehicleId: String?,
    val vehicleType: String,
    val plateNumber: String,
    val vehicleLabel: String,
    val vehicleColor: String,
    val vehiclePhotoUri: String? = null,
    val vehicleDrawableKey: String = "car",
    val ownerUserId: String?,
    val ownerName: String,
    val ownerPhone: String,
    val attendantUserId: String,
    val attendantName: String,
    val entryTime: Long,
    val exitTime: Long? = null,
    val status: String,
    val feeFcfa: Int,
    val paymentMethod: String,
    val paymentStatus: String,
    val exitWithoutQr: Boolean = false,
    val exitAuditNote: String? = null,
    val physicalCheckConfirmed: Boolean = false
)

@Entity(tableName = "payment_records")
data class PaymentRecordEntity(
    @PrimaryKey val id: String,
    val sessionId: String?,
    val parkingId: String,
    val parkingName: String,
    val userId: String,
    val referenceCode: String,
    val purpose: String,
    val amountFcfa: Int,
    val providerFeeFcfa: Int = 0,
    val method: String,
    val status: String,
    val providerRef: String? = null,
    val attendantName: String,
    val createdAt: Long,
    val confirmedAt: Long? = null
)

@Entity(tableName = "manager_applications")
data class ManagerApplicationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val fullName: String,
    val birthDate: String,
    val phone: String,
    val email: String,
    val city: String,
    val neighborhood: String,
    val parkingName: String,
    val parkingAddress: String,
    val parkingCity: String,
    val parkingNeighborhood: String,
    val parkingType: String,
    val acceptedVehicleTypes: String,
    val estimatedDailyVolume: Int,
    val plannedAttendantsCount: Int,
    val applicantStatus: String,
    val idFrontUri: String?,
    val idBackUri: String?,
    val selfieUri: String?,
    val consentAccepted: Boolean,
    val status: String,
    val reviewNote: String? = null,
    val submittedAt: Long,
    val reviewedAt: Long? = null
)

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val id: String,
    val parkingId: String,
    val parkingName: String,
    val sessionId: String?,
    val sessionRef: String?,
    val attendantName: String,
    val type: String,
    val description: String,
    val status: String,
    val resolutionNote: String? = null,
    val createdAt: Long
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val category: String,
    val isRead: Boolean = false,
    val createdAt: Long
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val actorUserId: String,
    val actorName: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val details: String,
    val createdAt: Long
)
