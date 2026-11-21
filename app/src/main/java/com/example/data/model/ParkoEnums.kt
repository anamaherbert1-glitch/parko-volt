package com.example.data.model

enum class ThemeMode(val label: String) {
    SYSTEM("Système"),
    LIGHT("Clair"),
    DARK("Sombre")
}

enum class AppSpace(val label: String) {
    PERSONAL("Personnel"),
    PRO("Professionnel"),
    ADMIN("Administration")
}

enum class UserRole(val label: String) {
    USER("Utilisateur"),
    ATTENDANT("Gardien"),
    SUPERVISOR("Superviseur"),
    MANAGER("Gestionnaire"),
    SUPER_ADMIN("Super administrateur")
}

enum class VehicleType(val label: String, val defaultFeeFcfa: Int) {
    MOTO("Moto", 200),
    VOITURE("Voiture", 500),
    TRICYCLE("Tricycle", 300),
    CAMION("Camion", 1000),
    AUTRE("Autre", 400)
}

enum class SessionStatus(val label: String) {
    ACTIVE("Valide"),
    EXIT_PENDING("Sortie en cours"),
    COMPLETED("Terminée"),
    EXPIRED("Expirée"),
    NEEDS_VERIFICATION("Vérification requise")
}

enum class RecoveryStatus(val label: String) {
    PENDING("En attente"),
    APPROVED("Approuvée"),
    REJECTED("Refusée")
}

enum class PaymentMethod(val label: String) {
    CASH("Espèces"),
    MIXX_BY_YAS("Mixx by Yas"),
    FLOOZ("Flooz")
}

enum class PaymentStatus(val label: String) {
    PENDING("En attente"),
    CONFIRMED("Confirmé"),
    FAILED("Échoué"),
    CANCELLED("Annulé"),
    REFUNDED("Remboursé")
}

enum class ApplicationStatus(val label: String) {
    PENDING("En attente"),
    UNDER_REVIEW("En cours d'examen"),
    MORE_INFO_REQUIRED("Infos requises"),
    APPROVED("Approuvée"),
    REJECTED("Rejetée")
}

enum class SubscriptionTier(
    val title: String,
    val priceFcfa: Int,
    val periodLabel: String,
    val quotaLabel: String,
    val quotaLimit: Int, // Int.MAX_VALUE for unlimited
    val maxAttendants: Int,
    val allowsPhotos: Boolean,
    val allowsExports: Boolean,
    val highlights: List<String>
) {
    FREE_TRIAL(
        title = "Essai gratuit",
        priceFcfa = 0,
        periodLabel = "0 FCFA",
        quotaLabel = "50 stationnements / mois",
        quotaLimit = 50,
        maxAttendants = 1,
        allowsPhotos = false,
        allowsExports = false,
        highlights = listOf(
            "50 nouveaux stationnements / mois",
            "1 gardien",
            "Tickets QR & scan de sortie",
            "Historique simple"
        )
    ),
    ESSENTIEL(
        title = "Essentiel",
        priceFcfa = 500,
        periodLabel = "500 FCFA/mois",
        quotaLabel = "15 véhicules / jour",
        quotaLimit = 15,
        maxAttendants = 1,
        allowsPhotos = false,
        allowsExports = false,
        highlights = listOf(
            "15 nouveaux stationnements / jour",
            "1 gardien • Motos & voitures",
            "Tickets QR & validation entrées/sorties",
            "Historique simple"
        )
    ),
    STANDARD(
        title = "Standard",
        priceFcfa = 3000,
        periodLabel = "3 000 FCFA/mois",
        quotaLabel = "50 véhicules / jour",
        quotaLimit = 50,
        maxAttendants = 3,
        allowsPhotos = true,
        allowsExports = false,
        highlights = listOf(
            "50 véhicules / jour • Jusqu'à 3 gardiens",
            "Photos des véhicules incluses",
            "Rapports journaliers & stats mensuelles",
            "Gestion complète des paiements"
        )
    ),
    PRO_UNLIMITED(
        title = "Pro Unlimited",
        priceFcfa = 5000,
        periodLabel = "5 000 FCFA/mois",
        quotaLabel = "Illimité",
        quotaLimit = Int.MAX_VALUE,
        maxAttendants = 7,
        allowsPhotos = true,
        allowsExports = true,
        highlights = listOf(
            "Stationnements & QR illimités",
            "Jusqu'à 7 gardiens interconnectés",
            "Classement, badges & gestion des incidents",
            "Export CSV & tableau de bord avancé"
        )
    );

    companion object {
        fun recommendedFor(dailyVolume: Int, attendantsCount: Int): SubscriptionTier {
            return when {
                dailyVolume > 50 || attendantsCount > 3 -> PRO_UNLIMITED
                dailyVolume > 15 || attendantsCount > 1 -> STANDARD
                else -> ESSENTIEL
            }
        }
    }
}

enum class ParkingSizePreset(
    val titleFr: String,
    val titleEn: String,
    val capacityLabelFr: String,
    val capacityLabelEn: String,
    val defaultDailyVolume: Int,
    val defaultAttendants: Int
) {
    SMALL("Petit", "Small", "≤ 15 véh./j", "≤ 15 veh./day", 12, 1),
    MEDIUM("Moyen", "Medium", "16–50 véh./j", "16–50 veh./day", 35, 2),
    LARGE("Grand", "Large", "50+ véh./j", "50+ veh./day", 80, 5)
}

enum class IncidentType(val label: String) {
    INVALID_QR("QR code invalide"),
    ALREADY_USED_QR("QR code déjà utilisé"),
    LOST_TICKET("Perte du ticket"),
    VEHICLE_MISMATCH("Véhicule non conforme"),
    PAYMENT_DISPUTE("Désaccord sur le paiement"),
    REGISTRATION_ERROR("Erreur d'enregistrement"),
    CONTESTED_EXIT("Sortie contestée"),
    ATTENDANT_ISSUE("Incident impliquant un gardien")
}

enum class IncidentStatus(val label: String) {
    OPEN("Ouvert"),
    IN_REVIEW("En examen"),
    RESOLVED("Résolu")
}

enum class BadgeTier(val label: String) {
    BRONZE("Bronze"),
    ARGENT("Argent"),
    OR("Or"),
    EXPERT("Expert"),
    EXCELLENCE("Excellence")
}
