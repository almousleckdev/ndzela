interface NotificationTarget {
  label: string; // what the notification is about, in words
  href: string | null; // where its subject can be handled
  action: string;
}

const TARGETS: Record<string, NotificationTarget> = {
  CONTACT_MESSAGE: { label: "Message du site", href: "/inquiries", action: "Ouvrir les messages" },
  DRIVER_APPLICATION: { label: "Candidature chauffeur", href: "/inquiries", action: "Ouvrir les candidatures" },
  DRIVER_MESSAGE: { label: "Message d'un chauffeur", href: "/drivers", action: "Ouvrir les chauffeurs" },
  DRIVER_DELETION_REQUEST: { label: "Demande de suppression de compte", href: "/drivers", action: "Ouvrir les chauffeurs" },
  ARREARS_RED: { label: "Impayés", href: "/finance/arrears", action: "Ouvrir les impayés" },
  ACCOUNT_LOCKED: { label: "Compte verrouillé", href: "/users", action: "Ouvrir les utilisateurs" },
  USER_CREATED: { label: "Nouvel utilisateur", href: "/users", action: "Ouvrir les utilisateurs" },
  MFA_RESET: { label: "Sécurité du compte", href: "/profile", action: "Ouvrir mon profil" },
  PASSWORD_CHANGED: { label: "Sécurité du compte", href: "/profile", action: "Ouvrir mon profil" },
  ROLES_CHANGED: { label: "Rôles", href: "/profile", action: "Ouvrir mon profil" },
};

const FALLBACK: NotificationTarget = { label: "Notification", href: null, action: "" };

export const notificationTarget = (type: string): NotificationTarget => TARGETS[type] ?? FALLBACK;
