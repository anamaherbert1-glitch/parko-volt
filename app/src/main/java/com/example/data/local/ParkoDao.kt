package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkoDao {
    // Users
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone OR LOWER(email) = LOWER(:identifier) LIMIT 1")
    suspend fun findUserByIdentifier(phone: String, identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun findUserByPhone(phone: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    // Vehicles
    @Query("SELECT * FROM vehicles WHERE ownerUserId = :userId ORDER BY createdAt DESC")
    fun observeUserVehicles(userId: String): Flow<List<VehicleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVehicle(vehicle: VehicleEntity)

    @Query("DELETE FROM vehicles WHERE id = :vehicleId")
    suspend fun deleteVehicle(vehicleId: String)

    // Parkings
    @Query("SELECT * FROM parking_locations ORDER BY createdAt ASC")
    fun observeAllParkings(): Flow<List<ParkingLocationEntity>>

    @Query("SELECT * FROM parking_locations WHERE id = :parkingId LIMIT 1")
    suspend fun getParkingById(parkingId: String): ParkingLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertParking(parking: ParkingLocationEntity)

    // Attendants
    @Query("SELECT * FROM attendants WHERE parkingId = :parkingId ORDER BY joinedAt ASC")
    fun observeAttendantsForParking(parkingId: String): Flow<List<AttendantEntity>>

    @Query("SELECT * FROM attendants ORDER BY registrationsCount DESC")
    fun observeAllAttendants(): Flow<List<AttendantEntity>>

    @Query("SELECT * FROM attendants WHERE userId = :userId AND isActive = 1 LIMIT 1")
    suspend fun findActiveAttendantByUser(userId: String): AttendantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendant(attendant: AttendantEntity)

    // Parking Sessions
    @Query("SELECT * FROM parking_sessions ORDER BY entryTime DESC")
    fun observeAllSessions(): Flow<List<ParkingSessionEntity>>

    @Query("SELECT * FROM parking_sessions WHERE ownerUserId = :userId OR ownerPhone = :phone ORDER BY entryTime DESC")
    fun observeUserSessions(userId: String, phone: String): Flow<List<ParkingSessionEntity>>

    @Query("SELECT * FROM parking_sessions WHERE parkingId = :parkingId ORDER BY entryTime DESC")
    fun observeParkingSessions(parkingId: String): Flow<List<ParkingSessionEntity>>

    @Query("SELECT * FROM parking_sessions WHERE UPPER(qrToken) = UPPER(:token) OR UPPER(referenceCode) = UPPER(:token) OR UPPER(referenceCode) = UPPER(:withPrefix) LIMIT 1")
    suspend fun findSessionByQrOrRef(token: String, withPrefix: String): ParkingSessionEntity?

    @Query("SELECT * FROM parking_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ParkingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ParkingSessionEntity)

    // Payments
    @Query("SELECT * FROM payment_records ORDER BY createdAt DESC")
    fun observeAllPayments(): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeUserPayments(userId: String): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE parkingId = :parkingId ORDER BY createdAt DESC")
    fun observeParkingPayments(parkingId: String): Flow<List<PaymentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayment(payment: PaymentRecordEntity)

    // Manager Applications
    @Query("SELECT * FROM manager_applications ORDER BY submittedAt DESC")
    fun observeAllApplications(): Flow<List<ManagerApplicationEntity>>

    @Query("SELECT * FROM manager_applications WHERE userId = :userId ORDER BY submittedAt DESC LIMIT 1")
    fun observeUserApplication(userId: String): Flow<ManagerApplicationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApplication(application: ManagerApplicationEntity)

    // Incidents
    @Query("SELECT * FROM incidents ORDER BY createdAt DESC")
    fun observeAllIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE parkingId = :parkingId ORDER BY createdAt DESC")
    fun observeParkingIncidents(parkingId: String): Flow<List<IncidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncident(incident: IncidentEntity)

    // Notifications
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeUserNotifications(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsRead(userId: String)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC")
    fun observeAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)
}
