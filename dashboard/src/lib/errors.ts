import { ApiException } from "@/lib/api/api-error";

const CODE_MESSAGES: Record<string, string> = {
  ACCOUNT_LOCKED: "Compte temporairement verrouillé suite à trop de tentatives. Réessayez plus tard.",
  SESSION_EXPIRED: "Votre session a expiré. Veuillez recommencer.",
  OTP_COOLDOWN: "Veuillez patienter avant de demander un nouveau code.",
  INVALID_CURRENT_PASSWORD: "Le mot de passe actuel est incorrect.",
  PASSWORD_REUSED: "Le nouveau mot de passe doit être différent de l'actuel.",
  USER_EMAIL_TAKEN: "Un utilisateur avec cet email existe déjà.",
  ROLE_NAME_TAKEN: "Un rôle portant ce nom existe déjà.",
  ROLE_IN_USE: "Ce rôle est encore attribué à des utilisateurs.",
  ROLE_PROTECTED: "Le rôle SUPER_ADMIN ne peut pas être modifié.",
  CANNOT_MODIFY_SELF: "Vous ne pouvez pas effectuer cette action sur votre propre compte.",
  INSUFFICIENT_PRIVILEGES: "Vous ne pouvez pas accorder ou gérer des droits que vous ne possédez pas.",
  ACCESS_DENIED: "Vous n'avez pas la permission d'effectuer cette action.",
  RESOURCE_NOT_FOUND: "Élément introuvable. Actualisez la page.",
  IMAGE_TOO_LARGE: "L'image doit peser 2 Mo maximum.",
  INVALID_IMAGE: "Format non pris en charge. Utilisez une image PNG, JPEG ou WebP.",
  IMAGE_UNREADABLE: "Impossible de lire cette image. Essayez un fichier PNG, JPEG ou WebP.",
  INVALID_COUNTRY: "Ce pays n'existe pas ou n'est pas actif.",
  COUNTRY_REQUIRED: "Choisissez un pays pour effectuer cette action.",
  COUNTRY_NOT_ALLOWED: "Vous ne pouvez créer un utilisateur que dans votre propre pays.",
  COUNTRY_INACTIVE: "Votre pays n'est pas actif.",
  VEHICLE_NUMBER_TAKEN: "Ce numéro de véhicule est déjà utilisé.",
  VEHICLE_PLATE_TAKEN: "Cette immatriculation est déjà utilisée.",
  VEHICLE_ASSIGNED: "Ce véhicule est affecté à un chauffeur. Retirez-le d'abord.",
  VEHICLE_STATUS_MANAGED: "Le statut « affecté » se définit uniquement en affectant le véhicule à un chauffeur.",
  VEHICLE_HAS_HISTORY: "Ce véhicule a des paiements ou des recharges enregistrés : mettez-le hors service plutôt que de le supprimer.",
  VEHICLE_YEAR_INVALID: "L'année ne peut pas dépasser l'année prochaine.",
  CONTRACT_TEMPLATE_UNAVAILABLE: "Il n'existe pas encore de modèle de contrat pour ce pays.",
  CONTRACT_NOT_ACTIVE: "Seul un contrat actif peut être mis à jour.",
  CONTRACT_ALREADY_ACTIVE: "Ce chauffeur ou ce véhicule a déjà un contrat actif.",
  DEPOSIT_INVALID: "La caution doit être un montant entier supérieur à 0.",
  PAYMENT_REFERENCE_REQUIRED: "Indiquez la référence du paiement (obligatoire hors espèces).",
  NO_ACTIVE_CONTRACT: "Ce chauffeur n'a pas de contrat actif.",
  ENTRY_ALREADY_REVERSED: "Cette écriture a déjà été annulée.",
  ENTRY_NOT_REVERSIBLE: "Seul un paiement ou un délai accordé peut être annulé.",
  DATE_RANGE_INVALID: "La date de fin doit suivre la date de début.",
  CHARGING_TARGET_INVALID: "Choisissez un véhicule NDZELA ou saisissez une voiture publique, pas les deux.",
  VEHICLE_HAS_NO_DRIVER: "Ce véhicule n'a pas de chauffeur pour le moment.",
  BATTERY_LEVELS_INVALID: "Le niveau de batterie après la recharge ne peut pas être inférieur à celui d'avant.",
  STATION_NAME_TAKEN: "Une station porte déjà ce nom.",
  STATION_IN_USE: "Cette station a des recharges enregistrées : elle ne peut pas être supprimée.",
  REASON_REQUIRED: "Indiquez le motif.",
  PHOTO_REQUIRED: "La photo de profil est obligatoire.",
  INVALID_FILE: "Fichier non accepté : utilisez une image (JPG, PNG) ou un PDF.",
  FILE_TOO_LARGE: "Le fichier dépasse 5 Mo.",
  DRIVER_BLOCKED: "Ce chauffeur est bloqué.",
  DRIVER_TOO_YOUNG: "Le chauffeur doit avoir au moins 18 ans.",
  DRIVER_ID_TAKEN: "Un chauffeur avec ce numéro de pièce d'identité existe déjà.",
  DRIVER_LICENCE_TAKEN: "Un chauffeur avec ce numéro de permis existe déjà.",
  DRIVER_EMAIL_TAKEN: "Un chauffeur avec cet email existe déjà.",
  DRIVER_PHONE_TAKEN: "Un chauffeur avec ce numéro de téléphone existe déjà.",
  DRIVER_HAS_VEHICLE: "Ce chauffeur a déjà un véhicule.",
  DRIVER_HAS_NO_VEHICLE: "Ce chauffeur n'a pas de véhicule.",
  DRIVER_PURGE_BLOCKED: "Un autre enregistrement empêche la suppression de ce chauffeur.",
  INCIDENT_KIND_INVALID: "Un contrat signé après midi donne une demi-journée, pas un arrêt.",
  DAY_OUT_OF_CONTRACT: "Le premier jour facturé est le lendemain de la signature du contrat : choisissez ce jour ou un jour suivant (7 jours à venir au plus).",
  DRIVER_HAS_HISTORY: "Ce chauffeur a des paiements ou des recharges enregistrés : suspendez-le plutôt que de le supprimer.",
  VEHICLE_NOT_AVAILABLE: "Ce véhicule n'est pas disponible.",
  VEHICLE_ALREADY_ASSIGNED: "Ce véhicule vient d'être affecté à un autre chauffeur.",
  TRAINING_CENTER_NAME_TAKEN: "Un centre de formation avec ce nom existe déjà.",
  TRAINING_CENTER_IN_USE: "Ce centre est utilisé par des chauffeurs.",
  PHONE_INVALID: "Numéro de téléphone invalide.",
  VALIDATION_ERROR: "Certaines données sont invalides.",
};

export function isSessionExpired(error: unknown): boolean {
  return error instanceof ApiException && error.response.code === "SESSION_EXPIRED";
}

// Turns any thrown value into a French message for the user.
export function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiException) {
    return CODE_MESSAGES[error.response.code] ?? fallback;
  }
  return "Une erreur inattendue s'est produite.";
}
