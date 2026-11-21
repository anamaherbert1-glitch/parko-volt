package com.example.data.local

import com.example.data.model.ApplicationStatus
import com.example.data.model.BadgeTier
import com.example.data.model.IncidentStatus
import com.example.data.model.IncidentType
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.SessionStatus
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserRole
import com.example.data.model.VehicleType
import com.example.util.SecurityUtils

object SeedData {
    const val DEMO_USER_ID = "usr_amadou"
    const val CLIENT_USER_ID = "usr_codjo"
    const val ADMIN_USER_ID = "usr_admin"
    const val PARKING_GRAND_MARCHE_ID = "pkg_grand_marche"
    const val PARKING_TOKOIN_ID = "pkg_tokoin"

    suspend fun seedIfEmpty(dao: ParkoDao) {
        if (dao.getUserCount() > 0) return

        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        val day = 86_400_000L

        fun makeUser(
            id: String,
            first: String,
            last: String,
            phone: String,
            email: String,
            pass: String,
            role: UserRole,
            city: String = "Lomé",
            neighborhood: String = "Tokoin"
        ): UserEntity {
            val salt = SecurityUtils.generateSalt()
            return UserEntity(
                id = id,
                firstName = first,
                lastName = last,
                phone = phone,
                email = email,
                passwordHash = SecurityUtils.hashPassword(pass, salt),
                salt = salt,
                role = role.name,
                isAccountVerified = true,
                city = city,
                neighborhood = neighborhood,
                createdAt = now - 30 * day
            )
        }

        // 1. Seed Users
        val amadou = makeUser(
            id = DEMO_USER_ID,
            first = "Amadou",
            last = "Diallo",
            phone = "+228 90 12 34 56",
            email = "amadou.diallo@parko.tg",
            pass = "Parko2026!",
            role = UserRole.MANAGER,
            neighborhood = "Grand Marché"
        )
        val koffi = makeUser(
            id = "usr_koffi",
            first = "Koffi",
            last = "Mensah",
            phone = "+228 91 22 33 44",
            email = "koffi.mensah@parko.tg",
            pass = "Parko2026!",
            role = UserRole.ATTENDANT
        )
        val amina = makeUser(
            id = "usr_amina",
            first = "Amina",
            last = "Traoré",
            phone = "+228 92 44 55 66",
            email = "amina.traore@parko.tg",
            pass = "Parko2026!",
            role = UserRole.SUPERVISOR
        )
        val dohou = makeUser(
            id = "usr_dohou",
            first = "Dohou",
            last = "Komlan",
            phone = "+228 93 77 88 99",
            email = "dohou.komlan@parko.tg",
            pass = "Parko2026!",
            role = UserRole.ATTENDANT
        )
        val codjo = makeUser(
            id = "usr_codjo",
            first = "Codjo",
            last = "Agbeko",
            phone = "+228 98 11 22 33",
            email = "codjo.agbeko@parko.tg",
            pass = "Parko2026!",
            role = UserRole.USER,
            neighborhood = "Adidogomé"
        )
        val admin = makeUser(
            id = ADMIN_USER_ID,
            first = "Équipe",
            last = "PARKO Admin",
            phone = "+228 90 00 00 01",
            email = "admin@parko.tg",
            pass = "Admin2026!",
            role = UserRole.SUPER_ADMIN,
            neighborhood = "Plateau"
        )

        listOf(amadou, koffi, amina, dohou, codjo, admin).forEach { dao.upsertUser(it) }

        // 2. Seed Parkings
        dao.upsertParking(
            ParkingLocationEntity(
                id = PARKING_GRAND_MARCHE_ID,
                name = "Parking Grand Marché",
                address = "Rue du Commerce, Assigamé",
                city = "Lomé",
                neighborhood = "Assigamé",
                parkingType = "Parking commercial mixte",
                acceptedTypes = "Moto, Voiture, Tricycle",
                managerUserId = DEMO_USER_ID,
                subscriptionTier = SubscriptionTier.STANDARD.name,
                subscriptionActive = true,
                subscriptionExpiresAt = now + 24 * day,
                quotaUsed = 12,
                quotaLimit = 50
            )
        )
        dao.upsertParking(
            ParkingLocationEntity(
                id = PARKING_TOKOIN_ID,
                name = "Parking CHU Tokoin",
                address = "Boulevard de l'Hôpital",
                city = "Lomé",
                neighborhood = "Tokoin",
                parkingType = "Parking sécurisé 24/7",
                acceptedTypes = "Moto, Voiture, Tricycle, Camion",
                managerUserId = "usr_amina",
                subscriptionTier = SubscriptionTier.PRO_UNLIMITED.name,
                subscriptionActive = true,
                subscriptionExpiresAt = now + 18 * day,
                quotaUsed = 38,
                quotaLimit = Int.MAX_VALUE
            )
        )

        // 3. Seed Attendants for Grand Marché
        dao.upsertAttendant(
            AttendantEntity(
                id = "att_koffi",
                parkingId = PARKING_GRAND_MARCHE_ID,
                userId = "usr_koffi",
                fullName = "Koffi Mensah",
                phone = "+228 91 22 33 44",
                role = UserRole.ATTENDANT.name,
                isActive = true,
                registrationsCount = 148,
                scansCount = 142,
                exitsCount = 140,
                incidentsCount = 1,
                procedureScore = 98,
                badgeTier = BadgeTier.EXPERT.name,
                joinedAt = now - 25 * day
            )
        )
        dao.upsertAttendant(
            AttendantEntity(
                id = "att_amina",
                parkingId = PARKING_GRAND_MARCHE_ID,
                userId = "usr_amina",
                fullName = "Amina Traoré",
                phone = "+228 92 44 55 66",
                role = UserRole.SUPERVISOR.name,
                isActive = true,
                registrationsCount = 186,
                scansCount = 185,
                exitsCount = 184,
                incidentsCount = 0,
                procedureScore = 99,
                badgeTier = BadgeTier.EXCELLENCE.name,
                joinedAt = now - 28 * day
            )
        )
        dao.upsertAttendant(
            AttendantEntity(
                id = "att_dohou",
                parkingId = PARKING_GRAND_MARCHE_ID,
                userId = "usr_dohou",
                fullName = "Dohou Komlan",
                phone = "+228 93 77 88 99",
                role = UserRole.ATTENDANT.name,
                isActive = true,
                registrationsCount = 94,
                scansCount = 91,
                exitsCount = 89,
                incidentsCount = 2,
                procedureScore = 94,
                badgeTier = BadgeTier.OR.name,
                joinedAt = now - 15 * day
            )
        )

        // 4. Seed Vehicles for Amadou
        val corolla = VehicleEntity(
            id = "veh_corolla",
            ownerUserId = DEMO_USER_ID,
            ownerName = "Amadou Diallo",
            ownerPhone = "+228 90 12 34 56",
            type = VehicleType.VOITURE.name,
            plateNumber = "TG-1234-AA",
            brand = "Toyota",
            model = "Corolla",
            color = "Gris argent",
            drawableKey = "car",
            createdAt = now - 20 * day
        )
        val yamaha = VehicleEntity(
            id = "veh_yamaha",
            ownerUserId = DEMO_USER_ID,
            ownerName = "Amadou Diallo",
            ownerPhone = "+228 90 12 34 56",
            type = VehicleType.MOTO.name,
            plateNumber = "TG-5678-BB",
            brand = "Yamaha",
            model = "MT-07",
            color = "Noir & Bleu",
            drawableKey = "moto",
            createdAt = now - 12 * day
        )
        dao.upsertVehicle(corolla)
        dao.upsertVehicle(yamaha)

        // 5. Seed Parking Sessions (2 active for Amadou + 2 active for others + historical completed)
        val s1 = ParkingSessionEntity(
            id = "ses_1",
            referenceCode = "PK-8492GM",
            qrToken = "PKO-7F9A21BC84D03E11",
            parkingId = PARKING_GRAND_MARCHE_ID,
            parkingName = "Grand Marché",
            vehicleId = corolla.id,
            vehicleType = VehicleType.VOITURE.name,
            plateNumber = "TG-1234-AA",
            vehicleLabel = "Toyota Corolla",
            vehicleColor = "Gris argent",
            vehicleDrawableKey = "car",
            ownerUserId = DEMO_USER_ID,
            ownerName = "Amadou Diallo",
            ownerPhone = "+228 90 12 34 56",
            attendantUserId = "usr_koffi",
            attendantName = "Koffi Mensah",
            entryTime = now - 2 * hour,
            status = SessionStatus.ACTIVE.name,
            feeFcfa = 500,
            paymentMethod = PaymentMethod.MIXX_BY_YAS.name,
            paymentStatus = PaymentStatus.CONFIRMED.name
        )
        val s2 = ParkingSessionEntity(
            id = "ses_2",
            referenceCode = "PK-9104TK",
            qrToken = "PKO-3C8E40FA19B722D4",
            parkingId = PARKING_TOKOIN_ID,
            parkingName = "CHU Tokoin",
            vehicleId = yamaha.id,
            vehicleType = VehicleType.MOTO.name,
            plateNumber = "TG-5678-BB",
            vehicleLabel = "Yamaha MT-07",
            vehicleColor = "Noir & Bleu",
            vehicleDrawableKey = "moto",
            ownerUserId = DEMO_USER_ID,
            ownerName = "Amadou Diallo",
            ownerPhone = "+228 90 12 34 56",
            attendantUserId = "usr_amina",
            attendantName = "Amina Traoré",
            entryTime = now - 1 * hour,
            status = SessionStatus.ACTIVE.name,
            feeFcfa = 200,
            paymentMethod = PaymentMethod.CASH.name,
            paymentStatus = PaymentStatus.PENDING.name
        )
        val s3 = ParkingSessionEntity(
            id = "ses_3",
            referenceCode = "PK-4420GM",
            qrToken = "PKO-91D5C3AA208844FF",
            parkingId = PARKING_GRAND_MARCHE_ID,
            parkingName = "Grand Marché",
            vehicleId = null,
            vehicleType = VehicleType.MOTO.name,
            plateNumber = "TG-3310-CC",
            vehicleLabel = "Honda CB125",
            vehicleColor = "Rouge",
            vehicleDrawableKey = "moto",
            ownerUserId = null,
            ownerName = "Kossi Mawuli",
            ownerPhone = "+228 97 55 10 20",
            attendantUserId = "usr_koffi",
            attendantName = "Koffi Mensah",
            entryTime = now - 3 * hour,
            status = SessionStatus.ACTIVE.name,
            feeFcfa = 200,
            paymentMethod = PaymentMethod.FLOOZ.name,
            paymentStatus = PaymentStatus.CONFIRMED.name
        )
        val s4 = ParkingSessionEntity(
            id = "ses_4",
            referenceCode = "PK-5518GM",
            qrToken = "PKO-5A19F7BB44C21109",
            parkingId = PARKING_GRAND_MARCHE_ID,
            parkingName = "Grand Marché",
            vehicleId = null,
            vehicleType = VehicleType.TRICYCLE.name,
            plateNumber = "TG-7712-DD",
            vehicleLabel = "TVS King",
            vehicleColor = "Jaune",
            vehicleDrawableKey = "moto",
            ownerUserId = null,
            ownerName = "Yawovi Ségbé",
            ownerPhone = "+228 96 14 28 35",
            attendantUserId = "usr_dohou",
            attendantName = "Dohou Komlan",
            entryTime = now - 90 * 60_000L,
            status = SessionStatus.ACTIVE.name,
            feeFcfa = 300,
            paymentMethod = PaymentMethod.CASH.name,
            paymentStatus = PaymentStatus.PENDING.name
        )
        val s5 = ParkingSessionEntity(
            id = "ses_5",
            referenceCode = "PK-2201GM",
            qrToken = "PKO-1B44E8CC90A27731",
            parkingId = PARKING_GRAND_MARCHE_ID,
            parkingName = "Grand Marché",
            vehicleId = corolla.id,
            vehicleType = VehicleType.VOITURE.name,
            plateNumber = "TG-1234-AA",
            vehicleLabel = "Toyota Corolla",
            vehicleColor = "Gris argent",
            vehicleDrawableKey = "car",
            ownerUserId = DEMO_USER_ID,
            ownerName = "Amadou Diallo",
            ownerPhone = "+228 90 12 34 56",
            attendantUserId = "usr_koffi",
            attendantName = "Koffi Mensah",
            entryTime = now - 2 * day - 4 * hour,
            exitTime = now - 2 * day - 1 * hour,
            status = SessionStatus.COMPLETED.name,
            feeFcfa = 500,
            paymentMethod = PaymentMethod.CASH.name,
            paymentStatus = PaymentStatus.CONFIRMED.name,
            physicalCheckConfirmed = true
        )
        val s6 = ParkingSessionEntity(
            id = "ses_6",
            referenceCode = "PK-1890GM",
            qrToken = "PKO-8E21C6DD45B10982",
            parkingId = PARKING_GRAND_MARCHE_ID,
            parkingName = "Grand Marché",
            vehicleId = yamaha.id,
            vehicleType = VehicleType.MOTO.name,
            plateNumber = "TG-5678-BB",
            vehicleLabel = "Yamaha MT-07",
            vehicleColor = "Noir & Bleu",
            vehicleDrawableKey = "moto",
            ownerUserId = DEMO_USER_ID,
            ownerName = "Amadou Diallo",
            ownerPhone = "+228 90 12 34 56",
            attendantUserId = "usr_dohou",
            attendantName = "Dohou Komlan",
            entryTime = now - 5 * day - 3 * hour,
            exitTime = now - 5 * day,
            status = SessionStatus.COMPLETED.name,
            feeFcfa = 200,
            paymentMethod = PaymentMethod.FLOOZ.name,
            paymentStatus = PaymentStatus.CONFIRMED.name,
            physicalCheckConfirmed = true
        )

        listOf(s1, s2, s3, s4, s5, s6).forEach { dao.upsertSession(it) }

        // 6. Seed Payments (matching 124 500 FCFA monthly parking total in mockup + personal receipts)
        val payments = listOf(
            PaymentRecordEntity(
                id = "pay_1",
                sessionId = s1.id,
                parkingId = PARKING_GRAND_MARCHE_ID,
                parkingName = "Grand Marché",
                userId = DEMO_USER_ID,
                referenceCode = "REC-8492",
                purpose = "STATIONNEMENT",
                amountFcfa = 500,
                providerFeeFcfa = 15,
                method = PaymentMethod.MIXX_BY_YAS.name,
                status = PaymentStatus.CONFIRMED.name,
                providerRef = "MXY-992014",
                attendantName = "Koffi Mensah",
                createdAt = now - 2 * hour,
                confirmedAt = now - 2 * hour
            ),
            PaymentRecordEntity(
                id = "pay_2",
                sessionId = s2.id,
                parkingId = PARKING_TOKOIN_ID,
                parkingName = "CHU Tokoin",
                userId = DEMO_USER_ID,
                referenceCode = "REC-9104",
                purpose = "STATIONNEMENT",
                amountFcfa = 200,
                method = PaymentMethod.CASH.name,
                status = PaymentStatus.PENDING.name,
                attendantName = "Amina Traoré",
                createdAt = now - 1 * hour
            ),
            PaymentRecordEntity(
                id = "pay_3",
                sessionId = s5.id,
                parkingId = PARKING_GRAND_MARCHE_ID,
                parkingName = "Grand Marché",
                userId = DEMO_USER_ID,
                referenceCode = "REC-2201",
                purpose = "STATIONNEMENT",
                amountFcfa = 500,
                method = PaymentMethod.CASH.name,
                status = PaymentStatus.CONFIRMED.name,
                attendantName = "Koffi Mensah",
                createdAt = now - 2 * day - 1 * hour,
                confirmedAt = now - 2 * day - 1 * hour
            ),
            PaymentRecordEntity(
                id = "pay_4",
                sessionId = s6.id,
                parkingId = PARKING_GRAND_MARCHE_ID,
                parkingName = "Grand Marché",
                userId = DEMO_USER_ID,
                referenceCode = "REC-1890",
                purpose = "STATIONNEMENT",
                amountFcfa = 200,
                providerFeeFcfa = 10,
                method = PaymentMethod.FLOOZ.name,
                status = PaymentStatus.CONFIRMED.name,
                providerRef = "FLZ-448120",
                attendantName = "Dohou Komlan",
                createdAt = now - 5 * day,
                confirmedAt = now - 5 * day
            ),
            // Monthly Grand Marché revenue baseline (71 500 Cash + 52 000 Mobile Money + 1 000 above = 124 500 FCFA)
            PaymentRecordEntity(
                id = "pay_hist_cash",
                sessionId = null,
                parkingId = PARKING_GRAND_MARCHE_ID,
                parkingName = "Grand Marché",
                userId = "usr_koffi",
                referenceCode = "LOT-ESP-05",
                purpose = "STATIONNEMENT",
                amountFcfa = 71500,
                method = PaymentMethod.CASH.name,
                status = PaymentStatus.CONFIRMED.name,
                attendantName = "Koffi Mensah",
                createdAt = now - 3 * day,
                confirmedAt = now - 3 * day
            ),
            PaymentRecordEntity(
                id = "pay_hist_momo",
                sessionId = null,
                parkingId = PARKING_GRAND_MARCHE_ID,
                parkingName = "Grand Marché",
                userId = "usr_amina",
                referenceCode = "LOT-MM-05",
                purpose = "STATIONNEMENT",
                amountFcfa = 51800,
                providerFeeFcfa = 500,
                method = PaymentMethod.MIXX_BY_YAS.name,
                status = PaymentStatus.CONFIRMED.name,
                providerRef = "MXY-BATCH-05",
                attendantName = "Amina Traoré",
                createdAt = now - 4 * day,
                confirmedAt = now - 4 * day
            ),
            PaymentRecordEntity(
                id = "pay_sub_gm",
                sessionId = null,
                parkingId = PARKING_GRAND_MARCHE_ID,
                parkingName = "Grand Marché",
                userId = DEMO_USER_ID,
                referenceCode = "SUB-STD-001",
                purpose = "ABONNEMENT",
                amountFcfa = 3000,
                method = PaymentMethod.FLOOZ.name,
                status = PaymentStatus.CONFIRMED.name,
                providerRef = "FLZ-SUB-7781",
                attendantName = "Amadou Diallo",
                createdAt = now - 6 * day,
                confirmedAt = now - 6 * day
            ),
            PaymentRecordEntity(
                id = "pay_sub_tk",
                sessionId = null,
                parkingId = PARKING_TOKOIN_ID,
                parkingName = "CHU Tokoin",
                userId = "usr_amina",
                referenceCode = "SUB-PRO-002",
                purpose = "ABONNEMENT",
                amountFcfa = 5000,
                method = PaymentMethod.MIXX_BY_YAS.name,
                status = PaymentStatus.CONFIRMED.name,
                providerRef = "MXY-SUB-8890",
                attendantName = "Amina Traoré",
                createdAt = now - 12 * day,
                confirmedAt = now - 12 * day
            )
        )
        payments.forEach { dao.upsertPayment(it) }

        // 7. Seed Manager Applications (Amadou approved + Codjo pending for Super-Admin review!)
        dao.upsertApplication(
            ManagerApplicationEntity(
                id = "app_amadou",
                userId = DEMO_USER_ID,
                fullName = "Amadou Diallo",
                birthDate = "14/06/1988",
                phone = "+228 90 12 34 56",
                email = "amadou.diallo@parko.tg",
                city = "Lomé",
                neighborhood = "Assigamé",
                parkingName = "Parking Grand Marché",
                parkingAddress = "Rue du Commerce, Assigamé",
                parkingCity = "Lomé",
                parkingNeighborhood = "Assigamé",
                parkingType = "Parking commercial mixte",
                acceptedVehicleTypes = "Moto, Voiture, Tricycle",
                estimatedDailyVolume = 45,
                plannedAttendantsCount = 3,
                applicantStatus = "Propriétaire",
                idFrontUri = "verified_doc_front",
                idBackUri = "verified_doc_back",
                selfieUri = "verified_selfie",
                consentAccepted = true,
                status = ApplicationStatus.APPROVED.name,
                reviewNote = "Identité et justificatif d'exploitation validés.",
                submittedAt = now - 29 * day,
                reviewedAt = now - 29 * day
            )
        )
        dao.upsertApplication(
            ManagerApplicationEntity(
                id = "app_codjo",
                userId = "usr_codjo",
                fullName = "Codjo Agbeko",
                birthDate = "22/09/1991",
                phone = "+228 98 11 22 33",
                email = "codjo.agbeko@parko.tg",
                city = "Lomé",
                neighborhood = "Adidogomé",
                parkingName = "Parking Carrefour Adidogomé",
                parkingAddress = "Route de Kpalimé, Adidogomé",
                parkingCity = "Lomé",
                parkingNeighborhood = "Adidogomé",
                parkingType = "Parking motos & tricycles",
                acceptedVehicleTypes = "Moto, Tricycle",
                estimatedDailyVolume = 35,
                plannedAttendantsCount = 2,
                applicantStatus = "Gestionnaire",
                idFrontUri = "pending_doc_front",
                idBackUri = "pending_doc_back",
                selfieUri = "pending_selfie",
                consentAccepted = true,
                status = ApplicationStatus.PENDING.name,
                submittedAt = now - 25 * 60_000L
            )
        )

        // 8. Seed Incidents
        dao.upsertIncident(
            IncidentEntity(
                id = "inc_1",
                parkingId = PARKING_GRAND_MARCHE_ID,
                parkingName = "Grand Marché",
                sessionId = s4.id,
                sessionRef = s4.referenceCode,
                attendantName = "Dohou Komlan",
                type = IncidentType.LOST_TICKET.name,
                description = "Le client indique avoir égaré son téléphone, vérification d'identité demandée avant sortie.",
                status = IncidentStatus.OPEN.name,
                createdAt = now - 40 * 60_000L
            )
        )

        // 9. Seed Notifications
        val notifs = listOf(
            NotificationEntity(
                id = "not_1",
                userId = DEMO_USER_ID,
                title = "Ticket valide",
                body = "Votre ticket PK-8492GM pour Toyota Corolla est actif au Parking Grand Marché.",
                category = "TICKET",
                isRead = false,
                createdAt = now - 2 * hour
            ),
            NotificationEntity(
                id = "not_2",
                userId = DEMO_USER_ID,
                title = "Nouveau badge",
                body = "Votre équipe a atteint 98% de score de fiabilité ce mois-ci !",
                category = "BADGE",
                isRead = false,
                createdAt = now - 1 * day
            ),
            NotificationEntity(
                id = "not_3",
                userId = DEMO_USER_ID,
                title = "Paiement confirmé",
                body = "Votre abonnement Standard (3 000 FCFA/mois) est actif.",
                category = "PAYMENT",
                isRead = true,
                createdAt = now - 6 * day
            )
        )
        notifs.forEach { dao.upsertNotification(it) }

        // 10. Seed Audit Logs
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_1",
                actorUserId = ADMIN_USER_ID,
                actorName = "Équipe PARKO Admin",
                action = "VALIDATION_CANDIDATURE",
                targetType = "MANAGER_APPLICATION",
                targetId = "app_amadou",
                details = "Candidature approuvée pour Parking Grand Marché (Lomé)",
                createdAt = now - 29 * day
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_2",
                actorUserId = "usr_koffi",
                actorName = "Koffi Mensah",
                action = "ENTREE_VEHICULE",
                targetType = "PARKING_SESSION",
                targetId = s1.referenceCode,
                details = "Toyota Corolla TG-1234-AA enregistrée • Paiement Mixx by Yas confirmé",
                createdAt = now - 2 * hour
            )
        )
    }
}
