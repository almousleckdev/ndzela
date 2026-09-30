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
  VALIDATION_ERROR: "Certaines données sont invalides.",
};

export function isSessionExpired(error: unknown): boolean {
  return error instanceof ApiException && error.response.code === "SESSION_EXPIRED";
}

/**
 * Turns any thrown value into a French message for the user. Server messages are English and
 * technical, so they are never shown directly: known error codes are mapped, everything else
 * gets the caller's context-specific fallback.
 */
export function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiException) {
    return CODE_MESSAGES[error.response.code] ?? fallback;
  }
  return "Une erreur inattendue s'est produite.";
}
