package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ParkoRepository(
    private val dao: ParkoDao,
    private val prefs: SessionPreferences
) {
    val loggedInUserId: Flow<String?> = prefs.loggedInUserId
    val activeSpace: Flow<AppSpace> = prefs.activeSpace
    val language: Flow<String> = prefs.language
    val themeMode: Flow<ThemeMode> = prefs.themeMode

    suspend fun ensureSeeded() {
        SeedData.seedIfEmpty(dao)
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
    fun observeAllApplications(): Flow<List<ManagerApplicationEntity>> = dao.observeAllApplications()
    fun observeAllAuditLogs(): Flow<List<AuditLogEntity>> = dao.observeAllAuditLogs()

    suspend fun setActiveSpace(space: AppSpace) = prefs.setActiveSpace(space)
    suspend fun setLanguage(lang: String) = prefs.setLanguage(lang)
    suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)

    suspend fun login(identifier: String, password: String, rememberMe: Boolean): Result<UserEntity> {
        val cleanId = identifier.trim()
        val normalizedPhone = SecurityUtils.normalizeTogoPhone(cleanId)
        val user = dao.findUserByIdentifier(normalizedPhone, cleanId)
            ?: return Result.failure(IllegalArgumentException("Aucun compte trouvé pour cet identifiant."))
        if (user.isSuspended) {
            return Result.failure(IllegalStateException("Ce compte est actuellement suspendu par l'administration PARKO."))
        }
        if (!SecurityUtils.verifyPassword(password, user.salt, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Mot de passe incorrect."))
        }
        prefs.saveSession(user.id, rememberMe, AppSpace.PERSONAL)
        return Result.success(user)
    }

    suspend fun loginDemo(userId: String): Result<UserEntity> {
        val user = dao.getUserById(userId)
            ?: return Result.failure(IllegalStateException("Compte démo introuvable."))
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
        val user = UserEntity(
            id = "usr_${UUID.randomUUID().toString().take(8)}",
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            phone = normalizedPhone,
            email = email.trim(),
            passwordHash = SecurityUtils.hashPassword(password, salt),
            salt = salt,
            role = UserRole.USER.name,
            isAccountVerified = true
        )
        dao.upsertUser(user)
        dao.upsertNotification(
            NotificationEntity(
                id = "not_${UUID.randomUUID().toString().take(8)}",
                userId = user.id,
                title = "Bienvenue sur PARKO",
                body = "Votre compte personnel est prêt. Ajoutez votre premier véhicule dès maintenant.",
                category = "SYSTEM",
                createdAt = System.currentTimeMillis()
            )
        )
        prefs.saveSession(user.id, true, AppSpace.PERSONAL)
        return Result.success(user)
    }

    suspend fun resetPasswordByPhone(phoneRaw: String, newPassword: String): Result<Unit> {
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
            drawableKey = drawableKey
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
            submittedAt = System.currentTimeMillis()
        )
        dao.upsertApplication(app)
        dao.upsertNotification(
            NotificationEntity(
                id = "not_${UUID.randomUUID().toString().take(8)}",
                userId = user.id,
                title = "Candidature envoyée",
                body = "Votre dossier pour ${app.parkingName} est en attente de vérification PARKO.",
                category = "APPLICATION",
                createdAt = System.currentTimeMillis()
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = user.id,
                actorName = user.fullName,
                action = "SOUMISSION_CANDIDATURE",
                targetType = "MANAGER_APPLICATION",
                targetId = app.id,
                details = "Candidature déposée pour ${app.parkingName} (${app.parkingCity})",
                createdAt = System.currentTimeMillis()
            )
        )
        return Result.success(app)
    }

    suspend fun reviewApplication(
        admin: UserEntity,
        application: ManagerApplicationEntity,
        newStatus: ApplicationStatus,
        note: String
    ) {
        val now = System.currentTimeMillis()
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
            // Create parking if user doesn't already own one
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
                        quotaLimit = SubscriptionTier.FREE_TRIAL.quotaLimit
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
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = admin.id,
                actorName = admin.fullName,
                action = "DECISION_CANDIDATURE_${newStatus.name}",
                targetType = "MANAGER_APPLICATION",
                targetId = application.id,
                details = "${application.parkingName} • ${note.ifBlank { newStatus.label }}",
                createdAt = now
            )
        )
    }

    suspend fun activateSubscription(
        user: UserEntity,
        parking: ParkingLocationEntity,
        tier: SubscriptionTier,
        method: PaymentMethod,
        providerConfirmed: Boolean
    ): Result<PaymentRecordEntity?> {
        val now = System.currentTimeMillis()
        if (tier != SubscriptionTier.FREE_TRIAL && method != PaymentMethod.CASH && !providerConfirmed) {
            // Mobile Money starts PENDING until backend/provider confirms
            val pendingPay = PaymentRecordEntity(
                id = "pay_${UUID.randomUUID().toString().take(8)}",
                sessionId = null,
                parkingId = parking.id,
                parkingName = parking.name,
                userId = user.id,
                referenceCode = "SUB-${tier.name.take(3)}-${(100..999).random()}",
                purpose = "ABONNEMENT",
                amountFcfa = tier.priceFcfa,
                providerFeeFcfa = if (tier.priceFcfa > 0) (tier.priceFcfa * 0.02).toInt() else 0,
                method = method.name,
                status = PaymentStatus.PENDING.name,
                providerRef = "${method.name.take(3)}-WAIT-${(1000..9999).random()}",
                attendantName = user.fullName,
                createdAt = now
            )
            dao.upsertPayment(pendingPay)
            return Result.success(pendingPay)
        }

        // Confirmed (Free Trial, Cash validated, or Provider-confirmed Mobile Money)
        dao.upsertParking(
            parking.copy(
                subscriptionTier = tier.name,
                subscriptionActive = true,
                subscriptionExpiresAt = now + 30L * 86_400_000L,
                quotaLimit = tier.quotaLimit
            )
        )
        var paymentRecord: PaymentRecordEntity? = null
        if (tier.priceFcfa > 0) {
            paymentRecord = PaymentRecordEntity(
                id = "pay_${UUID.randomUUID().toString().take(8)}",
                sessionId = null,
                parkingId = parking.id,
                parkingName = parking.name,
                userId = user.id,
                referenceCode = "SUB-${tier.name.take(3)}-${(100..999).random()}",
                purpose = "ABONNEMENT",
                amountFcfa = tier.priceFcfa,
                providerFeeFcfa = if (method == PaymentMethod.CASH) 0 else (tier.priceFcfa * 0.02).toInt(),
                method = method.name,
                status = PaymentStatus.CONFIRMED.name,
                providerRef = if (method == PaymentMethod.CASH) null else "${method.name.take(3)}-OK-${(1000..9999).random()}",
                attendantName = user.fullName,
                createdAt = now,
                confirmedAt = now
            )
            dao.upsertPayment(paymentRecord)
        }
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
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = user.id,
                actorName = user.fullName,
                action = "ACTIVATION_ABONNEMENT",
                targetType = "PARKING",
                targetId = parking.id,
                details = "Offre ${tier.title} activée (${tier.priceFcfa} FCFA via ${method.label})",
                createdAt = now
            )
        )
        return Result.success(paymentRecord)
    }

    suspend fun confirmPendingSubscriptionPayment(admin: UserEntity, payment: PaymentRecordEntity, parking: ParkingLocationEntity?) {
        val now = System.currentTimeMillis()
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
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = admin.id,
                actorName = admin.fullName,
                action = "CONFIRMATION_PAIEMENT_ABONNEMENT",
                targetType = "PAYMENT",
                targetId = payment.referenceCode,
                details = "Paiement ${payment.amountFcfa} FCFA vérifié auprès du prestataire",
                createdAt = now
            )
        )
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
    ): Result<ParkingSessionEntity> {
        if (!parking.subscriptionActive || System.currentTimeMillis() > parking.subscriptionExpiresAt) {
            return Result.failure(IllegalStateException("Abonnement expiré : les nouvelles entrées sont bloquées. Les véhicules présents peuvent toujours sortir."))
        }
        if (parking.quotaUsed >= parking.quotaLimit) {
            return Result.failure(IllegalStateException("Quota atteint (${parking.quotaUsed}/${parking.quotaLimit}) : passez à l'offre supérieure pour enregistrer de nouvelles entrées."))
        }
        if (plateNumber.isBlank() || ownerName.isBlank()) {
            return Result.failure(IllegalArgumentException("La plaque d'immatriculation et le nom du propriétaire sont requis."))
        }
        val normalizedPhone = if (ownerPhoneRaw.isNotBlank()) SecurityUtils.normalizeTogoPhone(ownerPhoneRaw) else ""
        val matchedOwner = if (normalizedPhone.isNotBlank()) dao.findUserByPhone(normalizedPhone) else null
        val now = System.currentTimeMillis()
        val refCode = SecurityUtils.generateSessionReference()
        val qrToken = SecurityUtils.generateOpaqueQrToken()
        val paymentStatus = if (paymentMethod == PaymentMethod.CASH) PaymentStatus.CONFIRMED else PaymentStatus.PENDING

        val session = ParkingSessionEntity(
            id = "ses_${UUID.randomUUID().toString().take(8)}",
            referenceCode = refCode,
            qrToken = qrToken,
            parkingId = parking.id,
            parkingName = parking.name.removePrefix("Parking "),
            vehicleId = null,
            vehicleType = vehicleType.name,
            plateNumber = plateNumber.trim().uppercase(),
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
            paymentStatus = paymentStatus.name
        )
        dao.upsertSession(session)

        // Increment quota ONLY on new session creation (Rule 9)
        val newQuotaUsed = parking.quotaUsed + 1
        dao.upsertParking(parking.copy(quotaUsed = newQuotaUsed))

        // Record payment entry
        dao.upsertPayment(
            PaymentRecordEntity(
                id = "pay_${UUID.randomUUID().toString().take(8)}",
                sessionId = session.id,
                parkingId = parking.id,
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

        // Update attendant stats if actor is an attendant
        dao.findActiveAttendantByUser(actor.id)?.let { att ->
            dao.upsertAttendant(att.copy(registrationsCount = att.registrationsCount + 1))
        }

        // Notify matched owner
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

        // Warn manager if quota >= 80%
        if (parking.quotaLimit != Int.MAX_VALUE && newQuotaUsed >= (parking.quotaLimit * 0.8).toInt()) {
            dao.upsertNotification(
                NotificationEntity(
                    id = "not_${UUID.randomUUID().toString().take(8)}",
                    userId = parking.managerUserId,
                    title = "Quota bientôt atteint",
                    body = "${parking.name} a consommé $newQuotaUsed / ${parking.quotaLimit} stationnements.",
                    category = "QUOTA",
                    createdAt = now
                )
            )
        }

        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "ENTREE_VEHICULE",
                targetType = "PARKING_SESSION",
                targetId = refCode,
                details = "${session.vehicleLabel} (${session.plateNumber}) • ${paymentMethod.label} (${paymentStatus.label})",
                createdAt = now
            )
        )
        return Result.success(session)
    }

    /**
     * Allows a parking manager or attendant to send/push an existing QR ticket directly
     * to a client's phone/PARKO account so it is immediately stored in their Tickets tab.
     */
    suspend fun sendTicketToClient(
        actor: UserEntity,
        session: ParkingSessionEntity,
        clientPhoneRaw: String
    ): Result<ParkingSessionEntity> {
        if (!SecurityUtils.isValidTogoPhone(clientPhoneRaw)) {
            return Result.failure(IllegalArgumentException("Numéro togolais invalide (ex: +228 90 12 34 56)."))
        }
        val normalizedPhone = SecurityUtils.normalizeTogoPhone(clientPhoneRaw)
        val matchedOwner = dao.findUserByPhone(normalizedPhone)
        val updated = session.copy(
            ownerPhone = normalizedPhone,
            ownerUserId = matchedOwner?.id ?: session.ownerUserId,
            ownerName = matchedOwner?.fullName ?: session.ownerName
        )
        dao.upsertSession(updated)
        val now = System.currentTimeMillis()
        if (matchedOwner != null) {
            dao.upsertNotification(
                NotificationEntity(
                    id = "not_${UUID.randomUUID().toString().take(8)}",
                    userId = matchedOwner.id,
                    title = "Ticket QR reçu",
                    body = "Le gestionnaire de ${session.parkingName} vous a envoyé le ticket ${session.referenceCode} (${session.plateNumber}). Il est stocké dans vos Tickets.",
                    category = "TICKET",
                    createdAt = now
                )
            )
        }
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "ENVOI_TICKET_QR_CLIENT",
                targetType = "PARKING_SESSION",
                targetId = session.referenceCode,
                details = "QR envoyé au client $normalizedPhone",
                createdAt = now
            )
        )
        return Result.success(updated)
    }

    /**
     * Allows a user to receive/import a QR code or auto-generated claim code sent by a parking manager
     * and store it directly in their Tickets tab while preserving the manager's recorded vehicle info.
     */
    suspend fun claimTicketByCode(
        user: UserEntity,
        rawCode: String
    ): Result<ParkingSessionEntity> {
        val code = rawCode.trim().replace(" ", "")
        if (code.isBlank()) {
            return Result.failure(IllegalArgumentException("Veuillez saisir le code généré par le gestionnaire."))
        }
        val upper = code.uppercase()
        val withPrefix = if (upper.startsWith("PK-") || upper.startsWith("PKO-")) upper else "PK-$upper"
        val session = dao.findSessionByQrOrRef(upper, withPrefix)
            ?: return Result.failure(IllegalArgumentException("Aucun ticket trouvé pour le code « $upper »."))
        val updated = session.copy(
            ownerUserId = user.id,
            ownerPhone = user.phone,
            ownerName = session.ownerName.ifBlank { user.fullName }
        )
        dao.upsertSession(updated)
        dao.upsertNotification(
            NotificationEntity(
                id = "not_${UUID.randomUUID().toString().take(8)}",
                userId = user.id,
                title = "Ticket QR reçu",
                body = "Le ticket ${updated.referenceCode} (${updated.vehicleLabel} • ${updated.plateNumber}) a été stocké dans vos Tickets.",
                category = "TICKET",
                createdAt = System.currentTimeMillis()
            )
        )
        return Result.success(updated)
    }

    /**
     * Scanning an active ticket retrieves the session WITHOUT consuming any quota (Rule 9).
     */
    suspend fun recordQrScan(actor: UserEntity, session: ParkingSessionEntity) {
        dao.findActiveAttendantByUser(actor.id)?.let { att ->
            dao.upsertAttendant(att.copy(scansCount = att.scansCount + 1))
        }
    }

    suspend fun verifyMobileMoneySessionPayment(
        actor: UserEntity,
        session: ParkingSessionEntity,
        method: PaymentMethod
    ): ParkingSessionEntity {
        val now = System.currentTimeMillis()
        val updated = session.copy(
            paymentMethod = method.name,
            paymentStatus = PaymentStatus.CONFIRMED.name
        )
        dao.upsertSession(updated)
        dao.upsertPayment(
            PaymentRecordEntity(
                id = "pay_${UUID.randomUUID().toString().take(8)}",
                sessionId = session.id,
                parkingId = session.parkingId,
                parkingName = session.parkingName,
                userId = session.ownerUserId ?: actor.id,
                referenceCode = "REC-${session.referenceCode.removePrefix("PK-")}",
                purpose = "STATIONNEMENT",
                amountFcfa = session.feeFcfa,
                providerFeeFcfa = if (method == PaymentMethod.CASH) 0 else 15,
                method = method.name,
                status = PaymentStatus.CONFIRMED.name,
                providerRef = if (method == PaymentMethod.CASH) null else "${method.name.take(3)}-VER-${(10000..99999).random()}",
                attendantName = actor.fullName,
                createdAt = now,
                confirmedAt = now
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "CONFIRMATION_PAIEMENT",
                targetType = "PARKING_SESSION",
                targetId = session.referenceCode,
                details = "${session.feeFcfa} FCFA confirmés via ${method.label}",
                createdAt = now
            )
        )
        return updated
    }

    /**
     * Rule 7 & Rule 10: Exit ALWAYS works even if quota reached or subscription expired,
     * PROVIDED physicalCheckConfirmed is true and session is not already COMPLETED.
     */
    suspend fun authorizeExit(
        actor: UserEntity,
        session: ParkingSessionEntity,
        physicalCheckConfirmed: Boolean,
        withoutQr: Boolean,
        auditReason: String
    ): Result<ParkingSessionEntity> {
        if (session.status == SessionStatus.COMPLETED.name) {
            return Result.failure(IllegalStateException("Ce véhicule a déjà été récupéré (ticket déjà utilisé)."))
        }
        if (!physicalCheckConfirmed) {
            return Result.failure(IllegalArgumentException("Le contrôle physique du véhicule est obligatoire avant d'autoriser la sortie."))
        }
        if (withoutQr && auditReason.isBlank()) {
            return Result.failure(IllegalArgumentException("Veuillez préciser le motif de la sortie sans QR code."))
        }
        if (session.paymentStatus != PaymentStatus.CONFIRMED.name) {
            return Result.failure(IllegalStateException("Le paiement doit être confirmé avant de valider la sortie."))
        }

        val now = System.currentTimeMillis()
        val updated = session.copy(
            status = SessionStatus.COMPLETED.name,
            exitTime = now,
            exitWithoutQr = withoutQr,
            exitAuditNote = if (withoutQr) auditReason.trim() else null,
            physicalCheckConfirmed = true
        )
        dao.upsertSession(updated)

        dao.findActiveAttendantByUser(actor.id)?.let { att ->
            val nextExits = att.exitsCount + 1
            val nextBadge = when {
                nextExits >= 200 -> BadgeTier.EXCELLENCE
                nextExits >= 150 -> BadgeTier.EXPERT
                nextExits >= 100 -> BadgeTier.OR
                nextExits >= 50 -> BadgeTier.ARGENT
                else -> BadgeTier.BRONZE
            }
            dao.upsertAttendant(
                att.copy(
                    exitsCount = nextExits,
                    badgeTier = nextBadge.name
                )
            )
        }

        if (session.ownerUserId != null) {
            dao.upsertNotification(
                NotificationEntity(
                    id = "not_${UUID.randomUUID().toString().take(8)}",
                    userId = session.ownerUserId,
                    title = "Stationnement terminé",
                    body = "Sortie validée pour ${session.vehicleLabel} (${session.plateNumber}). Reçu ${session.feeFcfa} FCFA disponible.",
                    category = "TICKET",
                    createdAt = now
                )
            )
        }

        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = if (withoutQr) "SORTIE_EXCEPTIONNELLE_SANS_QR" else "SORTIE_VALIDEE",
                targetType = "PARKING_SESSION",
                targetId = session.referenceCode,
                details = if (withoutQr) "Sortie sans QR • Motif: $auditReason" else "Sortie standard contrôlée pour ${session.plateNumber}",
                createdAt = now
            )
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
        if (description.isBlank()) {
            return Result.failure(IllegalArgumentException("Veuillez décrire l'incident."))
        }
        val now = System.currentTimeMillis()
        val incident = IncidentEntity(
            id = "inc_${UUID.randomUUID().toString().take(8)}",
            parkingId = parking.id,
            parkingName = parking.name.removePrefix("Parking "),
            sessionId = session?.id,
            sessionRef = session?.referenceCode,
            attendantName = actor.fullName,
            type = type.name,
            description = description.trim(),
            status = IncidentStatus.OPEN.name,
            createdAt = now
        )
        dao.upsertIncident(incident)
        if (session != null && session.status == SessionStatus.ACTIVE.name) {
            dao.upsertSession(session.copy(status = SessionStatus.NEEDS_VERIFICATION.name))
        }
        dao.upsertNotification(
            NotificationEntity(
                id = "not_${UUID.randomUUID().toString().take(8)}",
                userId = parking.managerUserId,
                title = "Incident signalé : ${type.label}",
                body = "${actor.fullName} a signalé un incident (${session?.referenceCode ?: parking.name}).",
                category = "INCIDENT",
                createdAt = now
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "SIGNALEMENT_INCIDENT",
                targetType = "INCIDENT",
                targetId = incident.id,
                details = "${type.label}: ${description.take(80)}",
                createdAt = now
            )
        )
        return Result.success(Unit)
    }

    suspend fun resolveIncident(actor: UserEntity, incident: IncidentEntity, note: String) {
        dao.upsertIncident(
            incident.copy(
                status = IncidentStatus.RESOLVED.name,
                resolutionNote = note.trim().ifBlank { "Résolu par le gestionnaire" }
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = actor.id,
                actorName = actor.fullName,
                action = "RESOLUTION_INCIDENT",
                targetType = "INCIDENT",
                targetId = incident.id,
                details = note.ifBlank { "Incident clôturé" },
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun inviteAttendant(
        manager: UserEntity,
        parking: ParkingLocationEntity,
        currentAttendants: List<AttendantEntity>,
        fullName: String,
        phoneRaw: String,
        role: UserRole
    ): Result<Unit> {
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
        val now = System.currentTimeMillis()
        val parts = fullName.trim().split(" ", limit = 2)
        val existingUser = dao.findUserByPhone(phone)
        val userId = existingUser?.id ?: run {
            val salt = SecurityUtils.generateSalt()
            val newUser = UserEntity(
                id = "usr_${UUID.randomUUID().toString().take(8)}",
                firstName = parts.firstOrNull().orEmpty(),
                lastName = parts.getOrNull(1).orEmpty(),
                phone = phone,
                email = "",
                passwordHash = SecurityUtils.hashPassword("Parko2026!", salt),
                salt = salt,
                role = role.name
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
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = manager.id,
                actorName = manager.fullName,
                action = "INVITATION_GARDIEN",
                targetType = "ATTENDANT",
                targetId = att.id,
                details = "$fullName ($phone) ajouté comme ${role.label} à ${parking.name}",
                createdAt = now
            )
        )
        return Result.success(Unit)
    }

    suspend fun toggleAttendantStatus(manager: UserEntity, attendant: AttendantEntity) {
        dao.upsertAttendant(attendant.copy(isActive = !attendant.isActive))
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = manager.id,
                actorName = manager.fullName,
                action = if (attendant.isActive) "SUSPENSION_GARDIEN" else "REACTIVATION_GARDIEN",
                targetType = "ATTENDANT",
                targetId = attendant.id,
                details = attendant.fullName,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleParkingSuspension(admin: UserEntity, parking: ParkingLocationEntity) {
        dao.upsertParking(parking.copy(isSuspended = !parking.isSuspended))
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = admin.id,
                actorName = admin.fullName,
                action = if (parking.isSuspended) "REACTIVATION_PARKING" else "SUSPENSION_PARKING",
                targetType = "PARKING",
                targetId = parking.id,
                details = parking.name,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleUserSuspension(admin: UserEntity, target: UserEntity) {
        if (target.id == admin.id) return
        dao.upsertUser(target.copy(isSuspended = !target.isSuspended))
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = admin.id,
                actorName = admin.fullName,
                action = if (target.isSuspended) "REACTIVATION_UTILISATEUR" else "SUSPENSION_UTILISATEUR",
                targetType = "USER",
                targetId = target.id,
                details = "${target.fullName} (${target.phone})",
                createdAt = System.currentTimeMillis()
            )
        )
    }
}
