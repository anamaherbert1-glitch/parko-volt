package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.util.ParkoSecurityConfig
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class EntryCreationResult(
    val session: ParkingSessionEntity,
    val plainWithdrawalCode: String
)

private data class LoginAttemptRecord(
    val failedCount: Int = 0,
    val delayUntil: Long = 0L,
    val hardLockedUntil: Long = 0L
)

class ParkoRepository(
    private val dao: ParkoDao,
    private val prefs: SessionPreferences,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    val loggedInUserId: Flow<String?> = prefs.loggedInUserId
    val activeSpace: Flow<AppSpace> = prefs.activeSpace
    val language: Flow<String> = prefs.language
    val themeMode: Flow<ThemeMode> = prefs.themeMode
    val proLastActiveAt: Flow<Long> = prefs.proLastActiveAt

    @Volatile
    var isOnlineProvider: () -> Boolean = { true }

    private val loginAttempts = ConcurrentHashMap<String, LoginAttemptRecord>()

    fun clearLoginRateLimitForTests() {
        loginAttempts.clear()
    }

    suspend fun ensureSeeded() {
        if (ParkoSecurityConfig.isDemoModeEnabled) {
            SeedData.seedIfEmpty(dao)
        }
    }

    fun observeUser(userId: String): Flow<UserEntity?> = dao.observeUserById(userId)
    fun observeAllUsers(): Flow<List<UserEntity>> = dao.observeAllUsers()
    fun observeUserVehicles(userId: String): Flow<List<VehicleEntity>> = dao.observeUserVehicles(userId)
    fun observeUserSessions(userId: String, phone: String): Flow<List<ParkingSessionEntity>> =
        dao.observeUserSessions(userId, phone)
    fun observeUserPayments(userId: String): Flow<List<PaymentRecordEntity>> = dao.observeUserPayments(userId)
    fun observeUserNotifications(userId: String): Flow<List<NotificationEntity>> = dao.observeUserNotifications(userId)
    fun observeUserApplication(userId: String): Flow<ManagerApplicationEntity?> = dao.observeUserApplication(userId)

    fun observeAllParkings(): Flow<List<ParkingLocationEntity>> = dao.observeAllParkings()
    fun observeParkingSessions(parkingId: String): Flow<List<ParkingSessionEntity>> =
        dao.observeParkingSessions(parkingId)
    fun observeAllSessions(): Flow<List<ParkingSessionEntity>> = dao.observeAllSessions()
    fun observeParkingPayments(parkingId: String): Flow<List<PaymentRecordEntity>> =
        dao.observeParkingPayments(parkingId)
    fun observeAllPayments(): Flow<List<PaymentRecordEntity>> = dao.observeAllPayments()
    fun observeAttendantsForParking(parkingId: String): Flow<List<AttendantEntity>> =
        dao.observeAttendantsForParking(parkingId)
    fun observeAllAttendants(): Flow<List<AttendantEntity>> = dao.observeAllAttendants()
    fun observeParkingIncidents(parkingId: String): Flow<List<IncidentEntity>> =
        dao.observeParkingIncidents(parkingId)
    fun observeAllIncidents(): Flow<List<IncidentEntity>> = dao.observeAllIncidents()
    fun observeParkingRecoveries(parkingId: String): Flow<List<ExceptionalRecoveryEntity>> =
        dao.observeParkingRecoveries(parkingId)
    fun observeAllApplications(): Flow<List<ManagerApplicationEntity>> = dao.observeAllApplications()
    fun observeAllAuditLogs(): Flow<List<AuditLogEntity>> = dao.observeAllAuditLogs()

    suspend fun setActiveSpace(space: AppSpace) = prefs.setActiveSpace(space)
    suspend fun setLanguage(lang: String) = prefs.setLanguage(lang)
    suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)
    suspend fun refreshProActivity() = prefs.refreshProActivity()

    private suspend fun writeAudit(
        actorUserId: String,
        actorName: String,
        action: String,
        targetType: String,
        targetId: String,
        details: String,
        parkingId: String? = null,
        sessionId: String? = null,
        result: String = "SUCCESS",
        reason: String? = null
    ) {
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = actorUserId,
                actorName = actorName,
                action = action,
                targetType = targetType,
                targetId = targetId,
                details = details,
                parkingId = parkingId,
                sessionId = sessionId,
                result = result,
                reason = reason,
                createdAt = nowProvider()
            )
        )
    }

    private suspend fun verifyActorAssignedToParking(
        actor: UserEntity,
        parkingId: String,
        allowedRoles: Set<UserRole>
    ): Result<ParkingLocationEntity> {
        if (actor.isSuspended) {
            writeAudit(
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "ACCES_REFUSE",
                targetType = "PARKING",
                targetId = parkingId,
                details = "Compte suspendu",
                parkingId = parkingId,
                result = "DENIED",
                reason = "Compte suspendu"
            )
            return Result.failure(IllegalStateException("Votre compte est actuellement suspendu."))
        }
        val role = UserRole.entries.firstOrNull { it.name == actor.role } ?: UserRole.USER
        if (role !in allowedRoles) {
            writeAudit(
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "ACCES_REFUSE",
                targetType = "PARKING",
                targetId = parkingId,
                details = "Rôle ${role.name} non autorisé pour cette action",
                parkingId = parkingId,
                result = "DENIED",
                reason = "Rôle insuffisant"
            )
            return Result.failure(SecurityException("Action non autorisée pour votre rôle (${role.label})."))
        }
        val parking = dao.getParkingById(parkingId)
            ?: return Result.failure(IllegalArgumentException("Parking introuvable."))
        if (parking.isSuspended) {
            writeAudit(
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "ACCES_REFUSE",
                targetType = "PARKING",
                targetId = parkingId,
                details = "Parking suspendu : ${parking.name}",
                parkingId = parkingId,
                result = "DENIED",
                reason = "Parking suspendu"
            )
            return Result.failure(IllegalStateException("Ce parking est suspendu par l'administration PARKO."))
        }
        val isMember = when (role) {
            UserRole.MANAGER -> parking.managerUserId == actor.id
            UserRole.ATTENDANT, UserRole.SUPERVISOR -> {
                val att = dao.findActiveAttendantByUser(actor.id)
                att != null && att.isActive && att.parkingId == parking.id
            }
            UserRole.SUPER_ADMIN, UserRole.USER -> false
        }
        if (!isMember) {
            writeAudit(
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "ACCES_REFUSE",
                targetType = "PARKING",
                targetId = parkingId,
                details = "Tentative d'accès inter-parking non autorisée sur ${parking.name}",
                parkingId = parkingId,
                result = "DENIED",
                reason = "Agent non rattaché à ce parking"
            )
            return Result.failure(SecurityException("Vous n'êtes pas rattaché au parking ${parking.name}."))
        }
        return Result.success(parking)
    }

    suspend fun checkProSessionExpired(actor: UserEntity?): Boolean {
        val currentSpace = prefs.activeSpace.first()
        if (currentSpace != AppSpace.PRO && currentSpace != AppSpace.ADMIN) return false
        val lastActive = prefs.proLastActiveAt.first()
        if (lastActive <= 0L) return false
        val now = nowProvider()
        if (now - lastActive > ParkoSecurityConfig.proSessionTimeoutMs) {
            prefs.setActiveSpace(AppSpace.PERSONAL)
            if (actor != null) {
                writeAudit(
                    actorUserId = actor.id,
                    actorName = actor.fullName,
                    action = "EXPIRATION_SESSION_PRO",
                    targetType = "SESSION",
                    targetId = actor.id,
                    details = "Session professionnelle expirée après inactivité",
                    result = "EXPIRED",
                    reason = "Timeout d'inactivité dépassé"
                )
            }
            return true
        }
        return false
    }

    suspend fun login(identifier: String, password: String, rememberMe: Boolean): Result<UserEntity> {
        val cleanId = identifier.trim()
        val key = if (cleanId.contains("@")) cleanId.lowercase() else SecurityUtils.normalizeTogoPhone(cleanId)
        val now = nowProvider()
        val attempt = loginAttempts[key] ?: LoginAttemptRecord()

        if (now < attempt.hardLockedUntil) {
            writeAudit(
                actorUserId = "anonymous",
                actorName = cleanId,
                action = "TENTATIVE_CONNEXION_REPETEE",
                targetType = "AUTH",
                targetId = key,
                details = "Connexion bloquée durant le verrouillage temporaire",
                result = "BLOCKED",
                reason = "Verrouillage après ${attempt.failedCount} échecs"
            )
            return Result.failure(
                IllegalStateException("Compte temporairement verrouillé suite à plusieurs échecs. Réessayez plus tard.")
            )
        }
        if (now < attempt.delayUntil) {
            return Result.failure(
                IllegalStateException("Délai de sécurité actif après plusieurs échecs. Patientez quelques secondes.")
            )
        }

        val normalizedPhone = SecurityUtils.normalizeTogoPhone(cleanId)
        val user = dao.findUserByIdentifier(normalizedPhone, cleanId)
        if (user == null) {
            recordFailedLogin(key, cleanId, now, attempt)
            return Result.failure(IllegalArgumentException("Aucun compte trouvé pour cet identifiant."))
        }
        if (user.isSuspended) {
            return Result.failure(IllegalStateException("Ce compte est actuellement suspendu par l'administration PARKO."))
        }
        if (!SecurityUtils.verifyPassword(password, user.salt, user.passwordHash)) {
            val next = recordFailedLogin(key, user.fullName, now, attempt)
            val msg = when {
                next.hardLockedUntil > now -> "Trop de tentatives incorrectes (${next.failedCount}). Compte temporairement verrouillé."
                next.delayUntil > now -> "Mot de passe incorrect. Délai de sécurité imposé avant la prochaine tentative."
                else -> "Mot de passe incorrect."
            }
            return Result.failure(IllegalArgumentException(msg))
        }

        loginAttempts.remove(key)
        prefs.saveSession(user.id, rememberMe, AppSpace.PERSONAL)
        writeAudit(
            actorUserId = user.id,
            actorName = user.fullName,
            action = "CONNEXION_REUSSIE",
            targetType = "AUTH",
            targetId = user.id,
            details = "Connexion authentifiée (${user.role})"
        )
        return Result.success(user)
    }

    private suspend fun recordFailedLogin(
        key: String,
        actorLabel: String,
        now: Long,
        prev: LoginAttemptRecord
    ): LoginAttemptRecord {
        val nextCount = prev.failedCount + 1
        val hardLock = if (nextCount >= ParkoSecurityConfig.loginHardLockThresholdAttempts) {
            now + ParkoSecurityConfig.loginHardLockDurationMs
        } else 0L
        val delay = if (hardLock == 0L && nextCount == ParkoSecurityConfig.loginDelayThresholdAttempts) {
            now + ParkoSecurityConfig.loginDelayDurationMs
        } else 0L
        val updated = LoginAttemptRecord(
            failedCount = nextCount,
            delayUntil = delay,
            hardLockedUntil = hardLock
        )
        loginAttempts[key] = updated
        writeAudit(
            actorUserId = "anonymous",
            actorName = actorLabel,
            action = if (hardLock > 0L) "TENTATIVE_CONNEXION_REPETEE" else "CONNEXION_ECHOUEE",
            targetType = "AUTH",
            targetId = key,
            details = "Échec de connexion #$nextCount",
            result = if (hardLock > 0L) "LOCKED" else "DENIED",
            reason = "Identifiants invalides"
        )
        return updated
    }

    suspend fun loginDemo(userId: String): Result<UserEntity> {
        if (!ParkoSecurityConfig.isDemoModeEnabled) {
            return Result.failure(SecurityException("Le mode démonstration est désactivé en production."))
        }
        val user = dao.getUserById(userId)
            ?: return Result.failure(IllegalStateException("Compte démo introuvable."))
        if (user.isSuspended) {
            return Result.failure(IllegalStateException("Ce compte est suspendu."))
        }
        val defaultSpace = if (user.role == UserRole.SUPER_ADMIN.name) AppSpace.ADMIN else AppSpace.PERSONAL
        prefs.saveSession(user.id, true, defaultSpace)
        return Result.success(user)
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        phoneRaw: String,
        email: String,
        password: String
    ): Result<UserEntity> {
        if (firstName.isBlank() || lastName.isBlank()) {
            return Result.failure(IllegalArgumentException("Le nom et le prénom sont obligatoires."))
        }
        if (!SecurityUtils.isValidTogoPhone(phoneRaw)) {
            return Result.failure(IllegalArgumentException("Veuillez saisir un numéro togolais valide (ex: +228 90 12 34 56)."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères."))
        }
        val normalizedPhone = SecurityUtils.normalizeTogoPhone(phoneRaw)
        if (dao.findUserByPhone(normalizedPhone) != null) {
            return Result.failure(IllegalStateException("Un compte existe déjà avec ce numéro de téléphone."))
        }
        val salt = SecurityUtils.generateSalt()
        val now = nowProvider()
        val user = UserEntity(
            id = "usr_${UUID.randomUUID().toString().take(8)}",
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            phone = normalizedPhone,
            email = email.trim(),
            passwordHash = SecurityUtils.hashPassword(password, salt),
            salt = salt,
            role = UserRole.USER.name,
            isAccountVerified = true,
            createdAt = now
        )
        dao.upsertUser(user)
        dao.upsertNotification(
            NotificationEntity(
                id = "not_${UUID.randomUUID().toString().take(8)}",
                userId = user.id,
                title = "Bienvenue sur PARKO",
                body = "Votre compte personnel est prêt. Ajoutez votre premier véhicule dès maintenant.",
                category = "SYSTEM",
                createdAt = now
            )
        )
        prefs.saveSession(user.id, true, AppSpace.PERSONAL)
        return Result.success(user)
    }

    suspend fun resetPasswordByPhone(phoneRaw: String, newPassword: String, otpCode: String): Result<Unit> {
        if (!ParkoSecurityConfig.isDemoModeEnabled) {
            return Result.failure(
                IllegalStateException("La réinitialisation par OTP local est désactivée en production sans service SMS vérifié.")
            )
        }
        if (otpCode.trim() != "1234") {
            return Result.failure(IllegalArgumentException("Code de vérification SMS incorrect."))
        }
        if (!SecurityUtils.isValidTogoPhone(phoneRaw)) {
            return Result.failure(IllegalArgumentException("Numéro togolais invalide."))
        }
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Minimum 6 caractères requis."))
        }
        val normalized = SecurityUtils.normalizeTogoPhone(phoneRaw)
        val user = dao.findUserByPhone(normalized)
            ?: return Result.failure(IllegalArgumentException("Aucun compte associé à ce numéro."))
        val salt = SecurityUtils.generateSalt()
        dao.upsertUser(
            user.copy(
                salt = salt,
                passwordHash = SecurityUtils.hashPassword(newPassword, salt)
            )
        )
        loginAttempts.remove(normalized)
        return Result.success(Unit)
    }

    suspend fun logout() {
        prefs.clearSession()
    }

    suspend fun updateProfile(user: UserEntity, firstName: String, lastName: String, city: String, neighborhood: String, notificationsEnabled: Boolean) {
        dao.upsertUser(
            user.copy(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                city = city.trim(),
                neighborhood = neighborhood.trim(),
                notificationsEnabled = notificationsEnabled
            )
        )
    }

    suspend fun saveVehicle(
        owner: UserEntity,
        vehicleId: String?,
        type: VehicleType,
        plate: String,
        brand: String,
        model: String,
        color: String,
        photoUri: String?
    ): Result<Unit> {
        if (plate.isBlank()) {
            return Result.failure(IllegalArgumentException("Le numéro d'immatriculation est requis."))
        }
        val drawableKey = if (type == VehicleType.MOTO || type == VehicleType.TRICYCLE) "moto" else "car"
        val entity = VehicleEntity(
            id = vehicleId ?: "veh_${UUID.randomUUID().toString().take(8)}",
            ownerUserId = owner.id,
            ownerName = owner.fullName,
            ownerPhone = owner.phone,
            type = type.name,
            plateNumber = plate.trim().uppercase(),
            brand = brand.trim(),
            model = model.trim(),
            color = color.trim(),
            photoUri = photoUri,
            drawableKey = drawableKey,
            createdAt = nowProvider()
        )
        dao.upsertVehicle(entity)
        return Result.success(Unit)
    }

    suspend fun deleteVehicle(vehicleId: String) {
        dao.deleteVehicle(vehicleId)
    }

    suspend fun markNotificationsRead(userId: String) {
        dao.markAllNotificationsRead(userId)
    }

    suspend fun submitManagerApplication(
        user: UserEntity,
        fullName: String,
        birthDate: String,
        phone: String,
        email: String,
        city: String,
        neighborhood: String,
        parkingName: String,
        parkingAddress: String,
        parkingCity: String,
        parkingNeighborhood: String,
        parkingType: String,
        acceptedVehicleTypes: String,
        dailyVolume: Int,
        attendantsCount: Int,
        applicantStatus: String,
        idFrontUri: String?,
        idBackUri: String?,
        selfieUri: String?,
        consentAccepted: Boolean
    ): Result<ManagerApplicationEntity> {
        if (parkingName.isBlank() || parkingAddress.isBlank() || fullName.isBlank()) {
            return Result.failure(IllegalArgumentException("Veuillez remplir tous les champs obligatoires."))
        }
        if (!consentAccepted) {
            return Result.failure(IllegalArgumentException("Vous devez accepter la politique de confidentialité des documents."))
        }
        val now = nowProvider()
        val app = ManagerApplicationEntity(
            id = "app_${UUID.randomUUID().toString().take(8)}",
            userId = user.id,
            fullName = fullName.trim(),
            birthDate = birthDate.trim(),
            phone = SecurityUtils.normalizeTogoPhone(phone),
            email = email.trim(),
            city = city.trim(),
            neighborhood = neighborhood.trim(),
            parkingName = parkingName.trim(),
            parkingAddress = parkingAddress.trim(),
            parkingCity = parkingCity.trim(),
            parkingNeighborhood = parkingNeighborhood.trim(),
            parkingType = parkingType,
            acceptedVehicleTypes = acceptedVehicleTypes,
            estimatedDailyVolume = dailyVolume,
            plannedAttendantsCount = attendantsCount,
            applicantStatus = applicantStatus,
            idFrontUri = idFrontUri ?: "doc_recto_fourni",
            idBackUri = idBackUri ?: "doc_verso_fourni",
            selfieUri = selfieUri ?: "selfie_fourni",
            consentAccepted = true,
            status = ApplicationStatus.PENDING.name,
            submittedAt = now
        )
        dao.upsertApplication(app)
        dao.upsertNotification(
            NotificationEntity(
                id = "not_${UUID.randomUUID().toString().take(8)}",
                userId = user.id,
                title = "Candidature envoyée",
                body = "Votre dossier pour ${app.parkingName} est en attente de vérification PARKO.",
                category = "APPLICATION",
                createdAt = now
            )
        )
        writeAudit(
            actorUserId = user.id,
            actorName = user.fullName,
            action = "SOUMISSION_CANDIDATURE",
            targetType = "MANAGER_APPLICATION",
            targetId = app.id,
            details = "Candidature déposée pour ${app.parkingName} (${app.parkingCity})"
        )
        return Result.success(app)
    }

    suspend fun reviewApplication(
        admin: UserEntity,
        application: ManagerApplicationEntity,
        newStatus: ApplicationStatus,
        note: String
    ): Result<Unit> {
        if (admin.role != UserRole.SUPER_ADMIN.name || admin.isSuspended) {
            writeAudit(
                actorUserId = admin.id,
                actorName = admin.fullName,
                action = "ACCES_REFUSE",
                targetType = "MANAGER_APPLICATION",
                targetId = application.id,
                details = "Tentative non autorisée de validation de candidature",
                result = "DENIED",
                reason = "Super administrateur requis"
            )
            return Result.failure(SecurityException("Action réservée au Super Administrateur PARKO."))
        }
        val now = nowProvider()
        dao.upsertApplication(
            application.copy(
                status = newStatus.name,
                reviewNote = note.trim().ifBlank { null },
                reviewedAt = now
            )
        )
        val applicant = dao.getUserById(application.userId)
        if (newStatus == ApplicationStatus.APPROVED && applicant != null) {
            dao.upsertUser(applicant.copy(role = UserRole.MANAGER.name))
            val existingParking = dao.getParkingById("pkg_${applicant.id}")
            if (existingParking == null) {
                dao.upsertParking(
                    ParkingLocationEntity(
                        id = "pkg_${applicant.id}",
                        name = application.parkingName,
                        address = application.parkingAddress,
                        city = application.parkingCity,
                        neighborhood = application.parkingNeighborhood,
                        parkingType = application.parkingType,
                        acceptedTypes = application.acceptedVehicleTypes,
                        managerUserId = applicant.id,
                        subscriptionTier = SubscriptionTier.FREE_TRIAL.name,
                        subscriptionActive = false,
                        subscriptionExpiresAt = now,
                        quotaUsed = 0,
                        quotaLimit = SubscriptionTier.FREE_TRIAL.quotaLimit,
                        createdAt = now
                    )
                )
            }
        }
        dao.upsertNotification(
            NotificationEntity(
                id = "not_${UUID.randomUUID().toString().take(8)}",
                userId = application.userId,
                title = when (newStatus) {
                    ApplicationStatus.APPROVED -> "Candidature approuvée !"
                    ApplicationStatus.REJECTED -> "Candidature refusée"
                    else -> "Mise à jour de candidature"
                },
                body = when (newStatus) {
                    ApplicationStatus.APPROVED -> "Félicitations ! Choisissez votre abonnement pour activer votre espace professionnel."
                    ApplicationStatus.REJECTED -> "Motif : ${note.ifBlank { "Documents non conformes" }}"
                    else -> note.ifBlank { "Votre dossier est en cours d'examen par l'équipe PARKO." }
                },
                category = "APPLICATION",
                createdAt = now
            )
        )
        writeAudit(
            actorUserId = admin.id,
            actorName = admin.fullName,
            action = "DECISION_CANDIDATURE_${newStatus.name}",
            targetType = "MANAGER_APPLICATION",
            targetId = application.id,
            details = "${application.parkingName} • ${note.ifBlank { newStatus.label }}"
        )
        return Result.success(Unit)
    }

    /**
     * Paid plans (Mobile Money or Cash) remain PENDING until validated by Super-Admin/provider.
     * Client code can NEVER self-activate a paid subscription.
     */
    suspend fun activateSubscription(
        user: UserEntity,
        parking: ParkingLocationEntity,
        tier: SubscriptionTier,
        method: PaymentMethod,
        @Suppress("UNUSED_PARAMETER") providerConfirmed: Boolean
    ): Result<PaymentRecordEntity?> {
        if (user.isSuspended || user.role != UserRole.MANAGER.name || parking.managerUserId != user.id) {
            writeAudit(
                actorUserId = user.id,
                actorName = user.fullName,
                action = "CONTOURNEMENT_ABONNEMENT",
                targetType = "PARKING",
                targetId = parking.id,
                details = "Tentative d'activation d'abonnement sans statut de gestionnaire propriétaire",
                parkingId = parking.id,
                result = "DENIED",
                reason = "Gestionnaire propriétaire requis"
            )
            return Result.failure(SecurityException("Seul le gestionnaire approuvé de ce parking peut gérer son abonnement."))
        }
        val now = nowProvider()

        // Any paid tier must NOT activate from client-side Mobile Money simulation.
        if (tier != SubscriptionTier.FREE_TRIAL && method != PaymentMethod.CASH) {
            val pendingPay = PaymentRecordEntity(
                id = "pay_${UUID.randomUUID().toString().take(8)}",
                sessionId = null,
                parkingId = parking.id,
                parkingName = parking.name,
                userId = user.id,
                referenceCode = "SUB-${tier.name.take(3)}-${(100..999).random()}",
                purpose = "ABONNEMENT",
                amountFcfa = tier.priceFcfa,
                providerFeeFcfa = (tier.priceFcfa * 0.02).toInt(),
                method = method.name,
                status = PaymentStatus.PENDING.name,
                providerRef = "${method.name.take(3)}-WAIT-${(1000..9999).random()}",
                attendantName = user.fullName,
                createdAt = now
            )
            dao.upsertPayment(pendingPay)
            writeAudit(
                actorUserId = user.id,
                actorName = user.fullName,
                action = "ABONNEMENT_EN_ATTENTE",
                targetType = "PAYMENT",
                targetId = pendingPay.referenceCode,
                details = "Demande d'abonnement ${tier.title} via ${method.label} en attente de validation serveur/admin",
                parkingId = parking.id,
                result = "PENDING"
            )
            return Result.success(pendingPay)
        }

        // Paid CASH subscription also waits for Super-Admin treasury confirmation; FREE_TRIAL activates directly.
        if (tier != SubscriptionTier.FREE_TRIAL && method == PaymentMethod.CASH) {
            val pendingCash = PaymentRecordEntity(
                id = "pay_${UUID.randomUUID().toString().take(8)}",
                sessionId = null,
                parkingId = parking.id,
                parkingName = parking.name,
                userId = user.id,
                referenceCode = "SUB-${tier.name.take(3)}-${(100..999).random()}",
                purpose = "ABONNEMENT",
                amountFcfa = tier.priceFcfa,
                providerFeeFcfa = 0,
                method = PaymentMethod.CASH.name,
                status = PaymentStatus.PENDING.name,
                providerRef = null,
                attendantName = user.fullName,
                createdAt = now
            )
            dao.upsertPayment(pendingCash)
            writeAudit(
                actorUserId = user.id,
                actorName = user.fullName,
                action = "ABONNEMENT_EN_ATTENTE",
                targetType = "PAYMENT",
                targetId = pendingCash.referenceCode,
                details = "Règlement espèces ${tier.title} en attente de validation PARKO Admin",
                parkingId = parking.id,
                result = "PENDING"
            )
            return Result.success(pendingCash)
        }

        // FREE_TRIAL activation for approved Manager
        dao.upsertParking(
            parking.copy(
                subscriptionTier = SubscriptionTier.FREE_TRIAL.name,
                subscriptionActive = true,
                subscriptionExpiresAt = now + 30L * 86_400_000L,
                quotaLimit = SubscriptionTier.FREE_TRIAL.quotaLimit
            )
        )
        dao.upsertNotification(
            NotificationEntity(
                id = "not_${UUID.randomUUID().toString().take(8)}",
                userId = user.id,
                title = "Abonnement ${tier.title} actif",
                body = "Votre espace professionnel pour ${parking.name} est activé.",
                category = "PAYMENT",
                createdAt = now
            )
        )
        writeAudit(
            actorUserId = user.id,
            actorName = user.fullName,
            action = "ACTIVATION_ABONNEMENT",
            targetType = "PARKING",
            targetId = parking.id,
            details = "Essai gratuit activé pour ${parking.name}",
            parkingId = parking.id
        )
        return Result.success(null)
    }

    suspend fun confirmPendingSubscriptionPayment(
        admin: UserEntity,
        payment: PaymentRecordEntity,
        parking: ParkingLocationEntity?
    ): Result<Unit> {
        if (admin.role != UserRole.SUPER_ADMIN.name || admin.isSuspended) {
            writeAudit(
                actorUserId = admin.id,
                actorName = admin.fullName,
                action = "ACCES_REFUSE",
                targetType = "PAYMENT",
                targetId = payment.referenceCode,
                details = "Tentative non autorisée de confirmation de paiement d'abonnement",
                result = "DENIED",
                reason = "Super administrateur requis"
            )
            return Result.failure(SecurityException("Action réservée au Super Administrateur PARKO."))
        }
        val now = nowProvider()
        dao.upsertPayment(
            payment.copy(
                status = PaymentStatus.CONFIRMED.name,
                confirmedAt = now
            )
        )
        if (parking != null) {
            val tier = SubscriptionTier.entries.firstOrNull { payment.amountFcfa == it.priceFcfa } ?: SubscriptionTier.STANDARD
            dao.upsertParking(
                parking.copy(
                    subscriptionTier = tier.name,
                    subscriptionActive = true,
                    subscriptionExpiresAt = now + 30L * 86_400_000L,
                    quotaLimit = tier.quotaLimit
                )
            )
        }
        writeAudit(
            actorUserId = admin.id,
            actorName = admin.fullName,
            action = "CONFIRMATION_PAIEMENT_ABONNEMENT",
            targetType = "PAYMENT",
            targetId = payment.referenceCode,
            details = "Paiement ${payment.amountFcfa} FCFA vérifié par l'administration PARKO",
            parkingId = parking?.id
        )
        return Result.success(Unit)
    }

    suspend fun registerVehicleEntry(
        actor: UserEntity,
        parking: ParkingLocationEntity,
        vehicleType: VehicleType,
        plateNumber: String,
        brandModel: String,
        color: String,
        ownerName: String,
        ownerPhoneRaw: String,
        feeFcfa: Int,
        paymentMethod: PaymentMethod,
        photoUri: String?
    ): Result<EntryCreationResult> {
        val authorizedParking = verifyActorAssignedToParking(
            actor = actor,
            parkingId = parking.id,
            allowedRoles = setOf(UserRole.MANAGER, UserRole.SUPERVISOR, UserRole.ATTENDANT)
        ).getOrElse { return Result.failure(it) }

        val now = nowProvider()
        if (!authorizedParking.subscriptionActive || now > authorizedParking.subscriptionExpiresAt) {
            return Result.failure(IllegalStateException("Abonnement expiré : les nouvelles entrées sont bloquées. Les véhicules présents peuvent toujours sortir."))
        }
        if (authorizedParking.quotaUsed >= authorizedParking.quotaLimit) {
            return Result.failure(IllegalStateException("Quota atteint (${authorizedParking.quotaUsed}/${authorizedParking.quotaLimit}) : passez à l'offre supérieure pour enregistrer de nouvelles entrées."))
        }
        val cleanPlate = plateNumber.trim().uppercase()
        if (cleanPlate.isBlank() || ownerName.isBlank()) {
            return Result.failure(IllegalArgumentException("La plaque d'immatriculation et le nom du propriétaire sont requis."))
        }
        val existingActive = dao.findActiveSessionByPlateInParking(authorizedParking.id, cleanPlate)
        if (existingActive != null) {
            return Result.failure(IllegalStateException("Un ticket actif (${existingActive.referenceCode}) existe déjà pour $cleanPlate dans ce parking."))
        }

        val normalizedPhone = if (ownerPhoneRaw.isNotBlank()) SecurityUtils.normalizeTogoPhone(ownerPhoneRaw) else ""
        val matchedOwner = if (normalizedPhone.isNotBlank()) dao.findUserByPhone(normalizedPhone) else null
        val refCode = SecurityUtils.generateSessionReference()
        val qrToken = SecurityUtils.generateOpaqueQrToken()
        val paymentStatus = if (paymentMethod == PaymentMethod.CASH) PaymentStatus.CONFIRMED else PaymentStatus.PENDING

        // Generate 6-digit confidential withdrawal code — NEVER stored in plaintext in DB.
        val plainWithdrawalCode = SecurityUtils.generateWithdrawalSecretCode()
        val codeSalt = SecurityUtils.generateSalt()
        val codeHash = SecurityUtils.hashSecret(plainWithdrawalCode, codeSalt)
        val encryptedOwnerCopy = SecurityUtils.encryptSensitiveText(plainWithdrawalCode)

        val session = ParkingSessionEntity(
            id = "ses_${UUID.randomUUID().toString().take(8)}",
            referenceCode = refCode,
            qrToken = qrToken,
            parkingId = authorizedParking.id,
            parkingName = authorizedParking.name.removePrefix("Parking "),
            vehicleId = null,
            vehicleType = vehicleType.name,
            plateNumber = cleanPlate,
            vehicleLabel = brandModel.trim().ifBlank { vehicleType.label },
            vehicleColor = color.trim().ifBlank { "Non précisée" },
            vehiclePhotoUri = photoUri,
            vehicleDrawableKey = if (vehicleType == VehicleType.MOTO || vehicleType == VehicleType.TRICYCLE) "moto" else "car",
            ownerUserId = matchedOwner?.id,
            ownerName = ownerName.trim(),
            ownerPhone = normalizedPhone,
            attendantUserId = actor.id,
            attendantName = actor.fullName,
            entryTime = now,
            status = SessionStatus.ACTIVE.name,
            feeFcfa = feeFcfa,
            paymentMethod = paymentMethod.name,
            paymentStatus = paymentStatus.name,
            withdrawalCodeSalt = codeSalt,
            withdrawalCodeHash = codeHash,
            encryptedWithdrawalCode = encryptedOwnerCopy,
            withdrawalCodeExpiresAt = now + ParkoSecurityConfig.withdrawalCodeValidityMs
        )
        dao.upsertSession(session)

        val newQuotaUsed = authorizedParking.quotaUsed + 1
        dao.upsertParking(authorizedParking.copy(quotaUsed = newQuotaUsed))

        dao.upsertPayment(
            PaymentRecordEntity(
                id = "pay_${UUID.randomUUID().toString().take(8)}",
                sessionId = session.id,
                parkingId = authorizedParking.id,
                parkingName = session.parkingName,
                userId = matchedOwner?.id ?: actor.id,
                referenceCode = "REC-${refCode.removePrefix("PK-")}",
                purpose = "STATIONNEMENT",
                amountFcfa = feeFcfa,
                providerFeeFcfa = if (paymentMethod == PaymentMethod.CASH) 0 else 15,
                method = paymentMethod.name,
                status = paymentStatus.name,
                providerRef = if (paymentMethod == PaymentMethod.CASH) null else "${paymentMethod.name.take(3)}-WAIT-${(1000..9999).random()}",
                attendantName = actor.fullName,
                createdAt = now,
                confirmedAt = if (paymentStatus == PaymentStatus.CONFIRMED) now else null
            )
        )

        dao.findActiveAttendantByUser(actor.id)?.let { att ->
            dao.upsertAttendant(att.copy(registrationsCount = att.registrationsCount + 1))
        }

        if (matchedOwner != null) {
            dao.upsertNotification(
                NotificationEntity(
                    id = "not_${UUID.randomUUID().toString().take(8)}",
                    userId = matchedOwner.id,
                    title = "Ticket QR généré",
                    body = "${session.vehicleLabel} (${session.plateNumber}) enregistré à ${session.parkingName}. Réf: $refCode.",
                    category = "TICKET",
                    createdAt = now
                )
            )
        }

        if (authorizedParking.quotaLimit != Int.MAX_VALUE && newQuotaUsed >= (authorizedParking.quotaLimit * 0.8).toInt()) {
            dao.upsertNotification(
                NotificationEntity(
                    id = "not_${UUID.randomUUID().toString().take(8)}",
                    userId = authorizedParking.managerUserId,
                    title = "Quota bientôt atteint",
                    body = "${authorizedParking.name} a consommé $newQuotaUsed / ${authorizedParking.quotaLimit} stationnements.",
                    category = "QUOTA",
                    createdAt = now
                )
            )
        }

        writeAudit(
            actorUserId = actor.id,
            actorName = actor.fullName,
            action = "ENTREE_VEHICULE",
            targetType = "PARKING_SESSION",
            targetId = refCode,
            details = "${session.vehicleLabel} (${session.plateNumber}) • ${paymentMethod.label} (${paymentStatus.label})",
            parkingId = authorizedParking.id,
            sessionId = session.id
        )
        return Result.success(EntryCreationResult(session = session, plainWithdrawalCode = plainWithdrawalCode))
    }

    suspend fun sendTicketToClient(
        actor: UserEntity,
        session: ParkingSessionEntity,
        clientPhoneRaw: String
    ): Result<ParkingSessionEntity> {
        verifyActorAssignedToParking(
            actor = actor,
            parkingId = session.parkingId,
            allowedRoles = setOf(UserRole.MANAGER, UserRole.SUPERVISOR, UserRole.ATTENDANT)
        ).getOrElse { return Result.failure(it) }

        if (!SecurityUtils.isValidTogoPhone(clientPhoneRaw)) {
            return Result.failure(IllegalArgumentException("Numéro togolais invalide (ex: +228 90 12 34 56)."))
        }
        val normalizedPhone = SecurityUtils.normalizeTogoPhone(clientPhoneRaw)
        val matchedOwner = dao.findUserByPhone(normalizedPhone)
        val fresh = dao.getSessionById(session.id) ?: session
        if (fresh.ownerUserId != null && matchedOwner != null && fresh.ownerUserId != matchedOwner.id) {
            return Result.failure(IllegalStateException("Ce ticket est déjà rattaché au compte d'un autre client."))
        }
        val updated = fresh.copy(
            ownerPhone = normalizedPhone,
            ownerUserId = matchedOwner?.id ?: fresh.ownerUserId,
            ownerName = matchedOwner?.fullName ?: fresh.ownerName
        )
        dao.upsertSession(updated)
        val now = nowProvider()
        if (matchedOwner != null) {
            dao.upsertNotification(
                NotificationEntity(
                    id = "not_${UUID.randomUUID().toString().take(8)}",
                    userId = matchedOwner.id,
                    title = "Ticket QR reçu",
                    body = "Le gestionnaire de ${updated.parkingName} vous a envoyé le ticket ${updated.referenceCode} (${updated.plateNumber}).",
                    category = "TICKET",
                    createdAt = now
                )
            )
        }
        writeAudit(
            actorUserId = actor.id,
            actorName = actor.fullName,
            action = "ENVOI_TICKET_QR_CLIENT",
            targetType = "PARKING_SESSION",
            targetId = updated.referenceCode,
            details = "Ticket associé au numéro client $normalizedPhone",
            parkingId = updated.parkingId,
            sessionId = updated.id
        )
        return Result.success(updated)
    }

    /**
     * Secured ticket claiming:
     * - Searches strictly by reference PK-XXXXYY (never by QR token).
     * - Refuses if already linked to another user.
     * - Requires matching recorded phone number (when present) AND matching plate number.
     */
    suspend fun claimTicketByCode(
        user: UserEntity,
        rawCode: String,
        verifyingPlate: String
    ): Result<ParkingSessionEntity> {
        val code = rawCode.trim().replace(" ", "").uppercase()
        if (code.isBlank()) {
            return Result.failure(IllegalArgumentException("Veuillez saisir la référence du ticket (PK-...)."))
        }
        if (code.startsWith("PKO-")) {
            writeAudit(
                actorUserId = user.id,
                actorName = user.fullName,
                action = "TENTATIVE_RATTACHEMENT_FRAUDULEUX",
                targetType = "PARKING_SESSION",
                targetId = code,
                details = "Tentative de rattachement par jeton QR au lieu de la référence PK",
                result = "DENIED",
                reason = "Jeton QR interdit pour le rattachement"
            )
            return Result.failure(IllegalArgumentException("Saisissez la référence du ticket (PK-...), pas un jeton QR."))
        }
        val cleanPlateInput = verifyingPlate.trim().uppercase().replace("-", "").replace(" ", "")
        if (cleanPlateInput.isBlank()) {
            return Result.failure(IllegalArgumentException("Saisissez la plaque d'immatriculation du véhicule pour vérification."))
        }
        val refToSearch = if (code.startsWith("PK-")) code else "PK-$code"
        val session = dao.findSessionByReference(refToSearch)
            ?: return Result.failure(IllegalArgumentException("Aucun ticket trouvé pour la référence « $refToSearch »."))

        if (session.ownerUserId != null && session.ownerUserId != user.id) {
            writeAudit(
                actorUserId = user.id,
                actorName = user.fullName,
                action = "TENTATIVE_RATTACHEMENT_FRAUDULEUX",
                targetType = "PARKING_SESSION",
                targetId = session.referenceCode,
                details = "Tentative de récupération d'un ticket appartenant déjà à ${session.ownerUserId}",
                parkingId = session.parkingId,
                sessionId = session.id,
                result = "DENIED",
                reason = "Ticket déjà associé à un autre compte"
            )
            return Result.failure(
                IllegalStateException("Ce ticket est déjà associé à un autre compte. Vous ne pouvez pas vous l'attribuer.")
            )
        }

        val sessionPlateClean = session.plateNumber.trim().uppercase().replace("-", "").replace(" ", "")
        if (sessionPlateClean != cleanPlateInput) {
            writeAudit(
                actorUserId = user.id,
                actorName = user.fullName,
                action = "TENTATIVE_RATTACHEMENT_FRAUDULEUX",
                targetType = "PARKING_SESSION",
                targetId = session.referenceCode,
                details = "Plaque saisie ($verifyingPlate) non conforme à celle du ticket (${session.plateNumber})",
                parkingId = session.parkingId,
                sessionId = session.id,
                result = "DENIED",
                reason = "Plaque non conforme"
            )
            return Result.failure(
                IllegalStateException("Les informations ne correspondent pas : plaque d'immatriculation incorrecte.")
            )
        }

        if (session.ownerPhone.isNotBlank()) {
            val recordedPhone = SecurityUtils.normalizeTogoPhone(session.ownerPhone)
            val callerPhone = SecurityUtils.normalizeTogoPhone(user.phone)
            if (recordedPhone != callerPhone) {
                writeAudit(
                    actorUserId = user.id,
                    actorName = user.fullName,
                    action = "TENTATIVE_RATTACHEMENT_FRAUDULEUX",
                    targetType = "PARKING_SESSION",
                    targetId = session.referenceCode,
                    details = "Numéro de téléphone du demandeur ($callerPhone) différent de l'entrée ($recordedPhone)",
                    parkingId = session.parkingId,
                    sessionId = session.id,
                    result = "DENIED",
                    reason = "Téléphone non conforme"
                )
                return Result.failure(
                    IllegalStateException("Les informations ne correspondent pas au numéro enregistré lors de l'entrée.")
                )
            }
        }

        val updated = if (session.ownerUserId == user.id) {
            session
        } else {
            session.copy(
                ownerUserId = user.id,
                ownerPhone = SecurityUtils.normalizeTogoPhone(user.phone),
                ownerName = session.ownerName.ifBlank { user.fullName }
            ).also { dao.upsertSession(it) }
        }
        val now = nowProvider()
        dao.upsertNotification(
            NotificationEntity(
                id = "not_${UUID.randomUUID().toString().take(8)}",
                userId = user.id,
                title = "Ticket QR reçu",
                body = "Le ticket ${updated.referenceCode} (${updated.vehicleLabel} • ${updated.plateNumber}) a été rattaché à votre compte.",
                category = "TICKET",
                createdAt = now
            )
        )
        writeAudit(
            actorUserId = user.id,
            actorName = user.fullName,
            action = "RATTACHEMENT_TICKET",
            targetType = "PARKING_SESSION",
            targetId = updated.referenceCode,
            details = "Ticket rattaché après vérification plaque & téléphone",
            parkingId = updated.parkingId,
            sessionId = updated.id
        )
        return Result.success(updated)
    }

    /**
     * Looks up a session strictly inside the actor's assigned parking.
     * Logs QR_REUTILISE if the scanned ticket is already COMPLETED.
     */
    suspend fun lookupParkingSessionForScan(
        actor: UserEntity,
        parkingId: String,
        query: String
    ): Result<ParkingSessionEntity> {
        verifyActorAssignedToParking(
            actor = actor,
            parkingId = parkingId,
            allowedRoles = setOf(UserRole.MANAGER, UserRole.SUPERVISOR, UserRole.ATTENDANT)
        ).getOrElse { return Result.failure(it) }

        val clean = query.trim()
        if (clean.isBlank()) {
            return Result.failure(IllegalArgumentException("Veuillez saisir une référence, une plaque ou scanner un QR."))
        }
        val upper = clean.uppercase()
        val withPrefix = if (upper.startsWith("PK-") || upper.startsWith("PKO-")) upper else "PK-$upper"
        var match = dao.findParkingSessionByQrOrRef(parkingId, upper, withPrefix)
        if (match == null) {
            val allInParking = dao.observeParkingSessions(parkingId).first()
            match = allInParking.firstOrNull {
                it.plateNumber.replace("-", "").equals(clean.replace("-", ""), ignoreCase = true) ||
                    (it.ownerPhone.isNotBlank() && it.ownerPhone.replace(" ", "") == clean.replace(" ", ""))
            }
        }
        if (match == null) {
            return Result.failure(IllegalArgumentException("Ticket introuvable dans ce parking."))
        }

        dao.findActiveAttendantByUser(actor.id)?.let { att ->
            if (att.parkingId == parkingId) {
                dao.upsertAttendant(att.copy(scansCount = att.scansCount + 1))
            }
        }

        if (match.status == SessionStatus.COMPLETED.name) {
            writeAudit(
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "QR_REUTILISE",
                targetType = "PARKING_SESSION",
                targetId = match.referenceCode,
                details = "Tentative de scan d'un ticket déjà utilisé pour une sortie (${match.plateNumber})",
                parkingId = parkingId,
                sessionId = match.id,
                result = "DENIED",
                reason = "Ticket déjà consommé"
            )
            return Result.success(match)
        }

        writeAudit(
            actorUserId = actor.id,
            actorName = actor.fullName,
            action = "SCAN_TICKET",
            targetType = "PARKING_SESSION",
            targetId = match.referenceCode,
            details = "Consultation du ticket ${match.referenceCode} pour vérification",
            parkingId = parkingId,
            sessionId = match.id
        )
        return Result.success(match)
    }

    /**
     * Acquires an exclusive EXIT_PENDING lock when opening vehicle exit verification.
     * Prevents simultaneous exit validation on two devices/agents.
     */
    suspend fun beginExitVerification(
        actor: UserEntity,
        session: ParkingSessionEntity
    ): Result<ParkingSessionEntity> {
        verifyActorAssignedToParking(
            actor = actor,
            parkingId = session.parkingId,
            allowedRoles = setOf(UserRole.MANAGER, UserRole.SUPERVISOR, UserRole.ATTENDANT)
        ).getOrElse { return Result.failure(it) }

        val fresh = dao.getSessionById(session.id)
            ?: return Result.failure(IllegalArgumentException("Ticket introuvable."))

        if (fresh.status == SessionStatus.COMPLETED.name) {
            writeAudit(
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "QR_REUTILISE",
                targetType = "PARKING_SESSION",
                targetId = fresh.referenceCode,
                details = "Ouverture d'un ticket déjà clôturé",
                parkingId = fresh.parkingId,
                sessionId = fresh.id,
                result = "DENIED",
                reason = "Sortie déjà effectuée"
            )
            return Result.success(fresh)
        }

        val now = nowProvider()
        if (fresh.status == SessionStatus.EXIT_PENDING.name &&
            fresh.exitPendingByUserId != null &&
            fresh.exitPendingByUserId != actor.id &&
            (fresh.exitPendingExpiresAt ?: 0L) > now
        ) {
            writeAudit(
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "SORTIE_CONCURRENTE_BLOQUEE",
                targetType = "PARKING_SESSION",
                targetId = fresh.referenceCode,
                details = "Verrou déjà détenu par ${fresh.exitPendingByName}",
                parkingId = fresh.parkingId,
                sessionId = fresh.id,
                result = "DENIED",
                reason = "Sortie déjà en cours par ${fresh.exitPendingByName}"
            )
            return Result.failure(
                IllegalStateException("Sortie déjà en cours par ${fresh.exitPendingByName ?: "un autre gardien"}.")
            )
        }

        if (fresh.status == SessionStatus.NEEDS_VERIFICATION.name || fresh.status == SessionStatus.EXPIRED.name) {
            return Result.success(fresh)
        }

        val locked = fresh.copy(
            status = SessionStatus.EXIT_PENDING.name,
            exitPendingByUserId = actor.id,
            exitPendingByName = actor.fullName,
            exitPendingAt = now,
            exitPendingExpiresAt = now + ParkoSecurityConfig.exitLockDurationMs
        )
        dao.upsertSession(locked)
        writeAudit(
            actorUserId = actor.id,
            actorName = actor.fullName,
            action = "DEMANDE_SORTIE",
            targetType = "PARKING_SESSION",
            targetId = locked.referenceCode,
            details = "Verrou de sortie EXIT_PENDING attribué à ${actor.fullName}",
            parkingId = locked.parkingId,
            sessionId = locked.id
        )
        return Result.success(locked)
    }

    suspend fun cancelExitVerification(actor: UserEntity, session: ParkingSessionEntity) {
        val fresh = dao.getSessionById(session.id) ?: return
        if (fresh.status == SessionStatus.EXIT_PENDING.name &&
            (fresh.exitPendingByUserId == actor.id ||
                actor.role == UserRole.MANAGER.name ||
                actor.role == UserRole.SUPERVISOR.name)
        ) {
            dao.upsertSession(
                fresh.copy(
                    status = SessionStatus.ACTIVE.name,
                    exitPendingByUserId = null,
                    exitPendingByName = null,
                    exitPendingAt = null,
                    exitPendingExpiresAt = null
                )
            )
        }
    }

    /**
     * Verifies the 6-digit secret withdrawal code against PBKDF2 hash.
     * Applies temporary cooldown at 3 failed attempts and requires Manager/Supervisor unlock at 5.
     */
    suspend fun verifyWithdrawalCode(
        actor: UserEntity,
        session: ParkingSessionEntity,
        inputCode: String
    ): Result<ParkingSessionEntity> {
        verifyActorAssignedToParking(
            actor = actor,
            parkingId = session.parkingId,
            allowedRoles = setOf(UserRole.MANAGER, UserRole.SUPERVISOR, UserRole.ATTENDANT)
        ).getOrElse { return Result.failure(it) }

        val fresh = dao.getSessionById(session.id)
            ?: return Result.failure(IllegalArgumentException("Ticket introuvable."))

        if (fresh.status == SessionStatus.COMPLETED.name) {
            return Result.failure(IllegalStateException("Ce ticket a déjà été utilisé pour une sortie. Toute nouvelle validation est interdite."))
        }
        if (fresh.withdrawalBlockedForManager) {
            writeAudit(
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "CODE_SECRET_BLOQUE",
                targetType = "PARKING_SESSION",
                targetId = fresh.referenceCode,
                details = "Tentative sur vérification bloquée (intervention gestionnaire requise)",
                parkingId = fresh.parkingId,
                sessionId = fresh.id,
                result = "BLOCKED",
                reason = "Blocage gestionnaire actif"
            )
            return Result.failure(
                IllegalStateException("Vérification bloquée après trop de tentatives. Intervention du gestionnaire ou du superviseur requise.")
            )
        }
        val now = nowProvider()
        if (fresh.withdrawalLockedUntil > now) {
            val remainingSec = ((fresh.withdrawalLockedUntil - now) / 1000L).coerceAtLeast(1L)
            return Result.failure(
                IllegalStateException("Délai de sécurité temporaire actif (${remainingSec}s restantes) suite à 3 codes erronés.")
            )
        }
        if (fresh.withdrawalCodeExpiresAt > 0L && now > fresh.withdrawalCodeExpiresAt) {
            writeAudit(
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "CODE_SECRET_EXPIRE",
                targetType = "PARKING_SESSION",
                targetId = fresh.referenceCode,
                details = "Code secret de retrait expiré",
                parkingId = fresh.parkingId,
                sessionId = fresh.id,
                result = "DENIED",
                reason = "Durée de validité dépassée"
            )
            return Result.failure(
                IllegalStateException("Code secret expiré. Utilisez « Client sans code de retrait » pour validation gestionnaire.")
            )
        }

        val cleanCode = inputCode.trim()
        val ok = SecurityUtils.verifySecret(cleanCode, fresh.withdrawalCodeSalt, fresh.withdrawalCodeHash)
        if (!ok) {
            val nextAttempts = fresh.withdrawalAttempts + 1
            val managerBlock = nextAttempts >= ParkoSecurityConfig.withdrawalManagerBlockAttempts
            val tempLock = if (!managerBlock && nextAttempts == ParkoSecurityConfig.withdrawalTempLockAttempts) {
                now + ParkoSecurityConfig.withdrawalTempLockDurationMs
            } else 0L
            val updated = fresh.copy(
                withdrawalAttempts = nextAttempts,
                withdrawalLockedUntil = tempLock,
                withdrawalBlockedForManager = managerBlock
            )
            dao.upsertSession(updated)
            writeAudit(
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = if (managerBlock) "CODE_SECRET_BLOQUE" else "CODE_SECRET_INCORRECT",
                targetType = "PARKING_SESSION",
                targetId = fresh.referenceCode,
                details = "Code secret de retrait incorrect (tentative #$nextAttempts)",
                parkingId = fresh.parkingId,
                sessionId = fresh.id,
                result = if (managerBlock) "BLOCKED" else "DENIED",
                reason = "Code secret invalide"
            )
            val errMsg = when {
                managerBlock -> "Blocage de sécurité après $nextAttempts tentatives incorrectes : faites intervenir le gestionnaire ou le superviseur."
                tempLock > 0L -> "Code secret incorrect ($nextAttempts tentatives) : délai de sécurité temporaire imposé."
                else -> "Code secret de retrait incorrect (tentative $nextAttempts/${ParkoSecurityConfig.withdrawalManagerBlockAttempts})."
            }
            return Result.failure(IllegalArgumentException(errMsg))
        }

        val verified = fresh.copy(
            withdrawalVerifiedAt = now,
            withdrawalVerifiedByUserId = actor.id,
            withdrawalAttempts = 0,
            withdrawalLockedUntil = 0L,
            withdrawalBlockedForManager = false
        )
        dao.upsertSession(verified)
        writeAudit(
            actorUserId = actor.id,
            actorName = actor.fullName,
            action = "CODE_SECRET_VERIFIE",
            targetType = "PARKING_SESSION",
            targetId = verified.referenceCode,
            details = "Autorisation de retrait par code secret vérifiée",
            parkingId = verified.parkingId,
            sessionId = verified.id
        )
        return Result.success(verified)
    }

    suspend fun unlockBlockedWithdrawal(
        actor: UserEntity,
        session: ParkingSessionEntity,
        reason: String
    ): Result<ParkingSessionEntity> {
        verifyActorAssignedToParking(
            actor = actor,
            parkingId = session.parkingId,
            allowedRoles = setOf(UserRole.MANAGER, UserRole.SUPERVISOR)
        ).getOrElse { return Result.failure(it) }

        if (reason.isBlank()) {
            return Result.failure(IllegalArgumentException("Un motif de déblocage est obligatoire."))
        }
        val fresh = dao.getSessionById(session.id)
            ?: return Result.failure(IllegalArgumentException("Ticket introuvable."))
        val updated = fresh.copy(
            withdrawalAttempts = 0,
            withdrawalLockedUntil = 0L,
            withdrawalBlockedForManager = false
        )
        dao.upsertSession(updated)
        writeAudit(
            actorUserId = actor.id,
            actorName = actor.fullName,
            action = "DEBLOCAGE_CODE_SECRET",
            targetType = "PARKING_SESSION",
            targetId = updated.referenceCode,
            details = "Déblocage effectué par ${actor.fullName}",
            parkingId = updated.parkingId,
            sessionId = updated.id,
            reason = reason.trim()
        )
        return Result.success(updated)
    }

    suspend fun reportPhysicalMismatch(
        actor: UserEntity,
        session: ParkingSessionEntity,
        description: String
    ): Result<ParkingSessionEntity> {
        val parking = verifyActorAssignedToParking(
            actor = actor,
            parkingId = session.parkingId,
            allowedRoles = setOf(UserRole.MANAGER, UserRole.SUPERVISOR, UserRole.ATTENDANT)
        ).getOrElse { return Result.failure(it) }

        val fresh = dao.getSessionById(session.id) ?: session
        val now = nowProvider()
        val updated = fresh.copy(
            physicalMismatchReported = true,
            status = SessionStatus.NEEDS_VERIFICATION.name,
            exitPendingByUserId = null,
            exitPendingByName = null,
            exitPendingAt = null,
            exitPendingExpiresAt = null
        )
        dao.upsertSession(updated)
        val inc = IncidentEntity(
            id = "inc_${UUID.randomUUID().toString().take(8)}",
            parkingId = parking.id,
            parkingName = parking.name.removePrefix("Parking "),
            sessionId = updated.id,
            sessionRef = updated.referenceCode,
            attendantName = actor.fullName,
            type = IncidentType.VEHICLE_MISMATCH.name,
            description = description.trim().ifBlank {
                "Non-conformité physique détectée sur ${updated.plateNumber} (${updated.vehicleLabel}). Sortie refusée."
            },
            status = IncidentStatus.OPEN.name,
            createdAt = now
        )
        dao.upsertIncident(inc)
        dao.upsertNotification(
            NotificationEntity(
                id = "not_${UUID.randomUUID().toString().take(8)}",
                userId = parking.managerUserId,
                title = "ALERTE : Véhicule non conforme",
                body = "${actor.fullName} a refusé la sortie de ${updated.referenceCode} (${updated.plateNumber}).",
                category = "INCIDENT",
                createdAt = now
            )
        )
        writeAudit(
            actorUserId = actor.id,
            actorName = actor.fullName,
            action = "CONTROLE_PHYSIQUE_NON_CONFORME",
            targetType = "PARKING_SESSION",
            targetId = updated.referenceCode,
            details = inc.description,
            parkingId = parking.id,
            sessionId = updated.id,
            result = "DENIED",
            reason = "Véhicule non conforme au ticket"
        )
        return Result.success(updated)
    }

    suspend fun submitExceptionalRecovery(
        actor: UserEntity,
        session: ParkingSessionEntity,
        reasonType: String,
        claimantName: String,
        declaredPlateNumber: String,
        description: String,
        physicalCheckConfirmed: Boolean
    ): Result<ExceptionalRecoveryEntity> {
        val parking = verifyActorAssignedToParking(
            actor = actor,
            parkingId = session.parkingId,
            allowedRoles = setOf(UserRole.MANAGER, UserRole.SUPERVISOR, UserRole.ATTENDANT)
        ).getOrElse { return Result.failure(it) }

        if (!physicalCheckConfirmed) {
            return Result.failure(IllegalArgumentException("La confirmation du contrôle physique du véhicule est obligatoire."))
        }
        if (claimantName.isBlank() || declaredPlateNumber.isBlank() || description.isBlank()) {
            return Result.failure(IllegalArgumentException("Tous les champs (identité, plaque, description détaillée) sont obligatoires."))
        }
        val fresh = dao.getSessionById(session.id)
            ?: return Result.failure(IllegalArgumentException("Ticket introuvable."))
        if (fresh.status == SessionStatus.COMPLETED.name) {
            return Result.failure(IllegalStateException("Ce véhicule est déjà sorti."))
        }
        val cleanDeclared = declaredPlateNumber.trim().uppercase().replace("-", "").replace(" ", "")
        val cleanActual = fresh.plateNumber.trim().uppercase().replace("-", "").replace(" ", "")
        if (cleanDeclared != cleanActual) {
            return Result.failure(IllegalStateException("La plaque déclarée ne correspond pas au ticket enregistré (${fresh.plateNumber})."))
        }

        val now = nowProvider()
        val recovery = ExceptionalRecoveryEntity(
            id = "rec_${UUID.randomUUID().toString().take(8)}",
            sessionId = fresh.id,
            sessionRef = fresh.referenceCode,
            parkingId = parking.id,
            parkingName = fresh.parkingName,
            requestedByUserId = actor.id,
            requestedByName = actor.fullName,
            reasonType = reasonType.trim(),
            claimantName = claimantName.trim(),
            declaredPlateNumber = declaredPlateNumber.trim().uppercase(),
            description = description.trim(),
            physicalCheckConfirmed = true,
            status = RecoveryStatus.PENDING.name,
            createdAt = now
        )
        dao.upsertRecovery(recovery)
        dao.upsertSession(fresh.copy(status = SessionStatus.NEEDS_VERIFICATION.name))
        dao.upsertNotification(
            NotificationEntity(
                id = "not_${UUID.randomUUID().toString().take(8)}",
                userId = parking.managerUserId,
                title = "Récupération exceptionnelle à valider",
                body = "${actor.fullName} demande une sortie sans code pour ${fresh.referenceCode} (${fresh.plateNumber}) — Motif : $reasonType.",
                category = "INCIDENT",
                createdAt = now
            )
        )
        writeAudit(
            actorUserId = actor.id,
            actorName = actor.fullName,
            action = "DEMANDE_RECUPERATION_EXCEPTIONNELLE",
            targetType = "EXCEPTIONAL_RECOVERY",
            targetId = recovery.id,
            details = "$reasonType • Demandeur: ${recovery.claimantName} (${recovery.declaredPlateNumber})",
            parkingId = parking.id,
            sessionId = fresh.id,
            result = "PENDING",
            reason = description.trim()
        )
        return Result.success(recovery)
    }

    /**
     * Strictly requires MANAGER or SUPERVISOR belonging to the parking.
     * An ATTENDANT can NEVER approve an exceptional recovery.
     */
    suspend fun reviewExceptionalRecovery(
        reviewer: UserEntity,
        recovery: ExceptionalRecoveryEntity,
        approve: Boolean,
        decisionReason: String
    ): Result<ExceptionalRecoveryEntity> {
        verifyActorAssignedToParking(
            actor = reviewer,
            parkingId = recovery.parkingId,
            allowedRoles = setOf(UserRole.MANAGER, UserRole.SUPERVISOR)
        ).getOrElse { return Result.failure(it) }

        if (decisionReason.isBlank()) {
            return Result.failure(IllegalArgumentException("Un motif de décision écrit est obligatoire."))
        }
        val now = nowProvider()
        val updatedRecovery = recovery.copy(
            status = if (approve) RecoveryStatus.APPROVED.name else RecoveryStatus.REJECTED.name,
            reviewedByUserId = reviewer.id,
            reviewedByName = reviewer.fullName,
            decisionReason = decisionReason.trim(),
            reviewedAt = now
        )
        dao.upsertRecovery(updatedRecovery)

        val session = dao.getSessionById(recovery.sessionId)
        if (session != null && session.status != SessionStatus.COMPLETED.name) {
            if (approve) {
                dao.upsertSession(
                    session.copy(
                        status = SessionStatus.ACTIVE.name,
                        physicalMismatchReported = false
                    )
                )
            }
        }
        writeAudit(
            actorUserId = reviewer.id,
            actorName = reviewer.fullName,
            action = if (approve) "APPROBATION_RECUPERATION_EXCEPTIONNELLE" else "REFUS_RECUPERATION_EXCEPTIONNELLE",
            targetType = "EXCEPTIONAL_RECOVERY",
            targetId = recovery.id,
            details = "Décision ${if (approve) "APPROUVÉE" else "REFUSÉE"} sur ${recovery.sessionRef}",
            parkingId = recovery.parkingId,
            sessionId = recovery.sessionId,
            result = if (approve) "SUCCESS" else "DENIED",
            reason = decisionReason.trim()
        )
        return Result.success(updatedRecovery)
    }

    suspend fun verifyMobileMoneySessionPayment(
        actor: UserEntity,
        session: ParkingSessionEntity,
        method: PaymentMethod
    ): Result<ParkingSessionEntity> {
        verifyActorAssignedToParking(
            actor = actor,
            parkingId = session.parkingId,
            allowedRoles = setOf(UserRole.MANAGER, UserRole.SUPERVISOR, UserRole.ATTENDANT)
        ).getOrElse { return Result.failure(it) }

        val fresh = dao.getSessionById(session.id) ?: session
        val now = nowProvider()
        val updated = fresh.copy(
            paymentMethod = method.name,
            paymentStatus = PaymentStatus.CONFIRMED.name
        )
        dao.upsertSession(updated)
        dao.upsertPayment(
            PaymentRecordEntity(
                id = "pay_${UUID.randomUUID().toString().take(8)}",
                sessionId = fresh.id,
                parkingId = fresh.parkingId,
                parkingName = fresh.parkingName,
                userId = fresh.ownerUserId ?: actor.id,
                referenceCode = "REC-${fresh.referenceCode.removePrefix("PK-")}",
                purpose = "STATIONNEMENT",
                amountFcfa = fresh.feeFcfa,
                providerFeeFcfa = if (method == PaymentMethod.CASH) 0 else 15,
                method = method.name,
                status = PaymentStatus.CONFIRMED.name,
                providerRef = if (method == PaymentMethod.CASH) null else "${method.name.take(3)}-VER-${(10000..99999).random()}",
                attendantName = actor.fullName,
                createdAt = now,
                confirmedAt = now
            )
        )
        writeAudit(
            actorUserId = actor.id,
            actorName = actor.fullName,
            action = "CONFIRMATION_PAIEMENT",
            targetType = "PARKING_SESSION",
            targetId = fresh.referenceCode,
            details = "${fresh.feeFcfa} FCFA confirmés via ${method.label}",
            parkingId = fresh.parkingId,
            sessionId = fresh.id
        )
        return Result.success(updated)
    }

    /**
     * Strict 9-condition exit authorization + offline check.
     * Exits NEVER consume entry quota and NEVER get blocked by quota exhaustion,
     * provided all 9 security checks hold.
     */
    suspend fun authorizeExit(
        actor: UserEntity,
        session: ParkingSessionEntity,
        physicalCheckConfirmed: Boolean,
        auditReason: String = ""
    ): Result<ParkingSessionEntity> {
        // 1. Ticket exists
        val fresh = dao.getSessionById(session.id)
            ?: return Result.failure(IllegalArgumentException("Ticket introuvable dans la base locale."))

        // 2. Actor & ticket belong to current authorized parking
        verifyActorAssignedToParking(
            actor = actor,
            parkingId = fresh.parkingId,
            allowedRoles = setOf(UserRole.MANAGER, UserRole.SUPERVISOR, UserRole.ATTENDANT)
        ).getOrElse { return Result.failure(it) }

        // Offline guard: never validate definitive exit while offline
        if (!isOnlineProvider()) {
            writeAudit(
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "SORTIE_REFUSEE_HORS_LIGNE",
                targetType = "PARKING_SESSION",
                targetId = fresh.referenceCode,
                details = "Tentative de validation de sortie définitive hors connexion",
                parkingId = fresh.parkingId,
                sessionId = fresh.id,
                result = "DENIED",
                reason = "Mode hors connexion"
            )
            return Result.failure(
                IllegalStateException("Mode hors connexion : la validation définitive de sortie est bloquée sans vérification réseau.")
            )
        }

        // 3 & 4. Vehicle is still present and ticket is not already COMPLETED / EXPIRED / NEEDS_VERIFICATION
        if (fresh.status == SessionStatus.COMPLETED.name || fresh.exitTime != null) {
            writeAudit(
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "QR_REUTILISE",
                targetType = "PARKING_SESSION",
                targetId = fresh.referenceCode,
                details = "Tentative de double sortie sur ${fresh.plateNumber}",
                parkingId = fresh.parkingId,
                sessionId = fresh.id,
                result = "DENIED",
                reason = "Ticket déjà consommé"
            )
            return Result.failure(
                IllegalStateException("Ce ticket a déjà été utilisé pour une sortie. Toute nouvelle validation est interdite.")
            )
        }
        if (fresh.status == SessionStatus.EXPIRED.name) {
            return Result.failure(IllegalStateException("Ce ticket est expiré."))
        }
        if (fresh.status == SessionStatus.NEEDS_VERIFICATION.name) {
            return Result.failure(IllegalStateException("Vérification obligatoire en attente : résolvez le blocage ou faites approuver par un gestionnaire."))
        }

        // 9. No competing EXIT_PENDING by another attendant
        val now = nowProvider()
        if (fresh.status == SessionStatus.EXIT_PENDING.name &&
            fresh.exitPendingByUserId != null &&
            fresh.exitPendingByUserId != actor.id &&
            (fresh.exitPendingExpiresAt ?: 0L) > now
        ) {
            return Result.failure(
                IllegalStateException("Une autre sortie est déjà en cours par ${fresh.exitPendingByName ?: "un autre gardien"}.")
            )
        }

        // 5. Physical check confirmed and no mismatch flagged
        if (!physicalCheckConfirmed || fresh.physicalMismatchReported) {
            return Result.failure(
                IllegalArgumentException("Attention : le contrôle physique conforme du véhicule est obligatoire avant la sortie.")
            )
        }

        // 8. No open blocking incident on this session
        val openIncidents = dao.getOpenIncidentsForSession(fresh.id)
        if (openIncidents.isNotEmpty()) {
            return Result.failure(
                IllegalStateException("Sortie refusée : ${openIncidents.size} incident(s) bloquant(s) non résolu(s) sur ce ticket.")
            )
        }

        // 7. Payment verified
        if (fresh.paymentStatus != PaymentStatus.CONFIRMED.name) {
            return Result.failure(IllegalStateException("Le paiement doit être confirmé avant de valider la sortie."))
        }

        // 6. Secret withdrawal code verified OR Manager/Supervisor approved exceptional recovery
        val latestRecovery = dao.getLatestRecoveryForSession(fresh.id)
        val hasApprovedRecovery = latestRecovery?.status == RecoveryStatus.APPROVED.name
        val hasVerifiedSecretCode = fresh.withdrawalVerifiedAt != null
        if (!hasVerifiedSecretCode && !hasApprovedRecovery) {
            writeAudit(
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "SORTIE_REFUSEE_SANS_CODE",
                targetType = "PARKING_SESSION",
                targetId = fresh.referenceCode,
                details = "Tentative de sortie sans validation du code secret ni récupération approuvée",
                parkingId = fresh.parkingId,
                sessionId = fresh.id,
                result = "DENIED",
                reason = "Code secret non vérifié"
            )
            return Result.failure(
                IllegalStateException("Vérification du code secret de retrait requise (ou récupération exceptionnelle approuvée).")
            )
        }

        // Complete exit and invalidate secret code copy so it can never be reused.
        val updated = fresh.copy(
            status = SessionStatus.COMPLETED.name,
            exitTime = now,
            exitWithoutQr = hasApprovedRecovery && !hasVerifiedSecretCode,
            exitAuditNote = if (hasApprovedRecovery && !hasVerifiedSecretCode) {
                (latestRecovery.decisionReason ?: auditReason).trim().ifBlank { "Récupération exceptionnelle approuvée" }
            } else null,
            physicalCheckConfirmed = true,
            encryptedWithdrawalCode = null,
            withdrawalCodeExpiresAt = now,
            exitPendingByUserId = null,
            exitPendingByName = null,
            exitPendingAt = null,
            exitPendingExpiresAt = null
        )
        dao.upsertSession(updated)

        dao.findActiveAttendantByUser(actor.id)?.let { att ->
            if (att.parkingId == fresh.parkingId) {
                val nextExits = att.exitsCount + 1
                val nextBadge = when {
                    nextExits >= 200 -> BadgeTier.EXCELLENCE
                    nextExits >= 150 -> BadgeTier.EXPERT
                    nextExits >= 100 -> BadgeTier.OR
                    nextExits >= 50 -> BadgeTier.ARGENT
                    else -> BadgeTier.BRONZE
                }
                dao.upsertAttendant(att.copy(exitsCount = nextExits, badgeTier = nextBadge.name))
            }
        }

        if (fresh.ownerUserId != null) {
            dao.upsertNotification(
                NotificationEntity(
                    id = "not_${UUID.randomUUID().toString().take(8)}",
                    userId = fresh.ownerUserId,
                    title = "Stationnement terminé",
                    body = "Sortie validée pour ${fresh.vehicleLabel} (${fresh.plateNumber}). Reçu ${fresh.feeFcfa} FCFA disponible.",
                    category = "TICKET",
                    createdAt = now
                )
            )
        }

        writeAudit(
            actorUserId = actor.id,
            actorName = actor.fullName,
            action = if (hasApprovedRecovery && !hasVerifiedSecretCode) "RECUPERATION_EXCEPTIONNELLE" else "SORTIE_VALIDEE",
            targetType = "PARKING_SESSION",
            targetId = updated.referenceCode,
            details = if (hasApprovedRecovery && !hasVerifiedSecretCode) {
                "Sortie exceptionnelle approuvée par ${latestRecovery.reviewedByName}"
            } else {
                "Sortie standard contrôlée pour ${updated.plateNumber} (code secret + contrôle physique)"
            },
            parkingId = updated.parkingId,
            sessionId = updated.id
        )
        return Result.success(updated)
    }

    suspend fun reportIncident(
        actor: UserEntity,
        parking: ParkingLocationEntity,
        session: ParkingSessionEntity?,
        type: IncidentType,
        description: String
    ): Result<Unit> {
        val authorizedParking = verifyActorAssignedToParking(
            actor = actor,
            parkingId = parking.id,
            allowedRoles = setOf(UserRole.MANAGER, UserRole.SUPERVISOR, UserRole.ATTENDANT)
        ).getOrElse { return Result.failure(it) }

        if (description.isBlank()) {
            return Result.failure(IllegalArgumentException("Veuillez décrire l'incident."))
        }
        val now = nowProvider()
        val incident = IncidentEntity(
            id = "inc_${UUID.randomUUID().toString().take(8)}",
            parkingId = authorizedParking.id,
            parkingName = authorizedParking.name.removePrefix("Parking "),
            sessionId = session?.id,
            sessionRef = session?.referenceCode,
            attendantName = actor.fullName,
            type = type.name,
            description = description.trim(),
            status = IncidentStatus.OPEN.name,
            createdAt = now
        )
        dao.upsertIncident(incident)
        if (session != null && (session.status == SessionStatus.ACTIVE.name || session.status == SessionStatus.EXIT_PENDING.name)) {
            dao.upsertSession(
                session.copy(
                    status = SessionStatus.NEEDS_VERIFICATION.name,
                    exitPendingByUserId = null,
                    exitPendingByName = null,
                    exitPendingAt = null,
                    exitPendingExpiresAt = null
                )
            )
        }
        dao.upsertNotification(
            NotificationEntity(
                id = "not_${UUID.randomUUID().toString().take(8)}",
                userId = authorizedParking.managerUserId,
                title = "Incident signalé : ${type.label}",
                body = "${actor.fullName} a signalé un incident (${session?.referenceCode ?: authorizedParking.name}).",
                category = "INCIDENT",
                createdAt = now
            )
        )
        writeAudit(
            actorUserId = actor.id,
            actorName = actor.fullName,
            action = "SIGNALEMENT_INCIDENT",
            targetType = "INCIDENT",
            targetId = incident.id,
            details = "${type.label}: ${description.take(80)}",
            parkingId = authorizedParking.id,
            sessionId = session?.id
        )
        return Result.success(Unit)
    }

    suspend fun resolveIncident(actor: UserEntity, incident: IncidentEntity, note: String): Result<Unit> {
        verifyActorAssignedToParking(
            actor = actor,
            parkingId = incident.parkingId,
            allowedRoles = setOf(UserRole.MANAGER, UserRole.SUPERVISOR)
        ).getOrElse { return Result.failure(it) }

        dao.upsertIncident(
            incident.copy(
                status = IncidentStatus.RESOLVED.name,
                resolutionNote = note.trim().ifBlank { "Résolu par le gestionnaire" }
            )
        )
        if (incident.sessionId != null) {
            val s = dao.getSessionById(incident.sessionId)
            if (s != null && s.status == SessionStatus.NEEDS_VERIFICATION.name && dao.getOpenIncidentsForSession(s.id).isEmpty()) {
                dao.upsertSession(s.copy(status = SessionStatus.ACTIVE.name, physicalMismatchReported = false))
            }
        }
        writeAudit(
            actorUserId = actor.id,
            actorName = actor.fullName,
            action = "RESOLUTION_INCIDENT",
            targetType = "INCIDENT",
            targetId = incident.id,
            details = note.ifBlank { "Incident clôturé" },
            parkingId = incident.parkingId,
            sessionId = incident.sessionId
        )
        return Result.success(Unit)
    }

    suspend fun inviteAttendant(
        manager: UserEntity,
        parking: ParkingLocationEntity,
        currentAttendants: List<AttendantEntity>,
        fullName: String,
        phoneRaw: String,
        role: UserRole
    ): Result<Unit> {
        verifyActorAssignedToParking(
            actor = manager,
            parkingId = parking.id,
            allowedRoles = setOf(UserRole.MANAGER)
        ).getOrElse { return Result.failure(it) }

        if (role != UserRole.ATTENDANT && role != UserRole.SUPERVISOR) {
            return Result.failure(IllegalArgumentException("Seuls les rôles Gardien ou Superviseur peuvent être attribués."))
        }

        val tier = SubscriptionTier.entries.firstOrNull { it.name == parking.subscriptionTier }
            ?: SubscriptionTier.FREE_TRIAL
        val activeCount = currentAttendants.count { it.isActive }
        if (activeCount >= tier.maxAttendants) {
            return Result.failure(
                IllegalStateException("Limite de gardiens atteinte ($activeCount/${tier.maxAttendants}) pour l'offre ${tier.title}.")
            )
        }
        if (fullName.isBlank() || !SecurityUtils.isValidTogoPhone(phoneRaw)) {
            return Result.failure(IllegalArgumentException("Nom complet et téléphone togolais valide requis."))
        }
        val phone = SecurityUtils.normalizeTogoPhone(phoneRaw)
        val now = nowProvider()
        val parts = fullName.trim().split(" ", limit = 2)
        val existingUser = dao.findUserByPhone(phone)
        val userId = existingUser?.id ?: run {
            val salt = SecurityUtils.generateSalt()
            val tempPass = SecurityUtils.generateWithdrawalSecretCode() + "Pk!"
            val newUser = UserEntity(
                id = "usr_${UUID.randomUUID().toString().take(8)}",
                firstName = parts.firstOrNull().orEmpty(),
                lastName = parts.getOrNull(1).orEmpty(),
                phone = phone,
                email = "",
                passwordHash = SecurityUtils.hashPassword(tempPass, salt),
                salt = salt,
                role = role.name,
                createdAt = now
            )
            dao.upsertUser(newUser)
            newUser.id
        }
        if (existingUser != null && existingUser.role == UserRole.USER.name) {
            dao.upsertUser(existingUser.copy(role = role.name))
        }
        val att = AttendantEntity(
            id = "att_${UUID.randomUUID().toString().take(8)}",
            parkingId = parking.id,
            userId = userId,
            fullName = fullName.trim(),
            phone = phone,
            role = role.name,
            isActive = true,
            badgeTier = BadgeTier.BRONZE.name,
            joinedAt = now
        )
        dao.upsertAttendant(att)
        writeAudit(
            actorUserId = manager.id,
            actorName = manager.fullName,
            action = "INVITATION_GARDIEN",
            targetType = "ATTENDANT",
            targetId = att.id,
            details = "$fullName ($phone) ajouté comme ${role.label} à ${parking.name}",
            parkingId = parking.id
        )
        return Result.success(Unit)
    }

    suspend fun toggleAttendantStatus(manager: UserEntity, attendant: AttendantEntity): Result<Unit> {
        verifyActorAssignedToParking(
            actor = manager,
            parkingId = attendant.parkingId,
            allowedRoles = setOf(UserRole.MANAGER)
        ).getOrElse { return Result.failure(it) }

        dao.upsertAttendant(attendant.copy(isActive = !attendant.isActive))
        writeAudit(
            actorUserId = manager.id,
            actorName = manager.fullName,
            action = if (attendant.isActive) "SUSPENSION_GARDIEN" else "REACTIVATION_GARDIEN",
            targetType = "ATTENDANT",
            targetId = attendant.id,
            details = attendant.fullName,
            parkingId = attendant.parkingId
        )
        return Result.success(Unit)
    }

    suspend fun toggleParkingSuspension(admin: UserEntity, parking: ParkingLocationEntity): Result<Unit> {
        if (admin.role != UserRole.SUPER_ADMIN.name || admin.isSuspended) {
            return Result.failure(SecurityException("Action réservée au Super Administrateur PARKO."))
        }
        dao.upsertParking(parking.copy(isSuspended = !parking.isSuspended))
        writeAudit(
            actorUserId = admin.id,
            actorName = admin.fullName,
            action = if (parking.isSuspended) "REACTIVATION_PARKING" else "SUSPENSION_PARKING",
            targetType = "PARKING",
            targetId = parking.id,
            details = parking.name,
            parkingId = parking.id
        )
        return Result.success(Unit)
    }

    suspend fun toggleUserSuspension(admin: UserEntity, target: UserEntity): Result<Unit> {
        if (admin.role != UserRole.SUPER_ADMIN.name || admin.isSuspended) {
            return Result.failure(SecurityException("Action réservée au Super Administrateur PARKO."))
        }
        if (target.id == admin.id) {
            return Result.failure(IllegalArgumentException("Impossible de suspendre votre propre compte."))
        }
        dao.upsertUser(target.copy(isSuspended = !target.isSuspended))
        writeAudit(
            actorUserId = admin.id,
            actorName = admin.fullName,
            action = if (target.isSuspended) "REACTIVATION_UTILISATEUR" else "SUSPENSION_UTILISATEUR",
            targetType = "USER",
            targetId = target.id,
            details = "${target.fullName} (${target.phone})"
        )
        return Result.success(Unit)
    }
}
