// Single source of truth for roles and permissions on the frontend.

export const PERMISSIONS = {
  USERS_READ: "users:read",
  USERS_CREATE: "users:create",
  USERS_UPDATE: "users:update",
  USERS_DEACTIVATE: "users:deactivate",
  USERS_LOCK: "users:lock",
  USERS_DELETE: "users:delete",
  USERS_EXPORT: "users:export",
  ROLES_READ: "roles:read",
  ROLES_CREATE: "roles:create",
  ROLES_UPDATE: "roles:update",
  ROLES_DELETE: "roles:delete",
  TRANSACTIONS_READ: "transactions:read",
  TRANSACTIONS_CREATE: "transactions:create",
  TRANSACTIONS_APPROVE: "transactions:approve",
  TRANSACTIONS_CANCEL: "transactions:cancel",
  ACCOUNTS_READ: "accounts:read",
  ACCOUNTS_CREATE: "accounts:create",
  ACCOUNTS_UPDATE: "accounts:update",
  REPORTS_READ: "reports:read",
  REPORTS_EXPORT: "reports:export",
  CASH_OPERATE: "cash:operate",
  COUNTRIES_READ: "countries:read",
  AUDIT_READ: "audit:read",
  AUDIT_EXPORT: "audit:export",
  VEHICLES_READ: "vehicles:read",
  VEHICLES_CREATE: "vehicles:create",
  VEHICLES_UPDATE: "vehicles:update",
  VEHICLES_DELETE: "vehicles:delete",
  VEHICLES_EXPORT: "vehicles:export",
  DRIVERS_READ: "drivers:read",
  DRIVERS_CREATE: "drivers:create",
  DRIVERS_UPDATE: "drivers:update",
  DRIVERS_SUSPEND: "drivers:suspend",
  DRIVERS_BLOCK: "drivers:block",
  DRIVERS_UNLOCK: "drivers:unlock",
  DRIVERS_EXPORT: "drivers:export",
  DRIVERS_DELETE: "drivers:delete",
  PAYMENTS_READ: "payments:read",
  PAYMENTS_CREATE: "payments:create",
  PAYMENTS_REVERSE: "payments:reverse",
  PAYMENTS_EXPORT: "payments:export",
  ARREARS_MANAGE: "arrears:manage",
  CHARGING_READ: "charging:read",
  CHARGING_CREATE: "charging:create",
  CHARGING_REVERSE: "charging:reverse",
  CHARGING_EXPORT: "charging:export",
  CHARGING_MANAGE: "charging:manage",
  CHARGING_LINK: "charging:link",
  CONTRACTS_READ: "contracts:read",
  CONTRACTS_PRINT: "contracts:print",
  CONTRACTS_UPDATE: "contracts:update",
  TRAINING_CENTERS_MANAGE: "training_centers:manage",
  INQUIRIES_READ: "inquiries:read",
  INQUIRIES_MANAGE: "inquiries:manage",
} as const;

export type Permission = (typeof PERMISSIONS)[keyof typeof PERMISSIONS];

export const ALL_PERMISSIONS: string[] = Object.values(PERMISSIONS);

const ROLE_LABELS: Record<string, string> = {
  SUPER_ADMIN: "Super administrateur",
  ADMIN: "Administrateur",
  GESTIONNAIRE: "Gestionnaire",
  COMPTABLE: "Comptable",
  CAISSIER: "Caissier",
  VIEWER: "Lecteur",
};

const ROLE_DESCRIPTIONS: Record<string, string> = {
  SUPER_ADMIN: "Contrôle total de la plateforme : utilisateurs, rôles, permissions et toutes les opérations financières.",
  ADMIN: "Gère les personnes : invite, modifie, suspend, bloque, débloque et supprime des utilisateurs. Ne touche pas aux opérations financières.",
  GESTIONNAIRE: "Supervise l'argent : crée, valide et annule des transactions, gère les comptes et exporte les rapports.",
  COMPTABLE: "Tient les livres : consulte les transactions, met à jour les comptes et produit les rapports. Ne peut pas déplacer d'argent.",
  CAISSIER: "Tient la caisse : enregistre les encaissements et décaissements et consulte les transactions.",
  VIEWER: "Lecture seule : consulte utilisateurs, rôles, transactions, comptes et rapports.",
};

// Known roles get a French title; custom roles like AUDITEUR_EXTERNE become "Auditeur externe".
export function roleLabel(role: string): string {
  if (ROLE_LABELS[role]) return ROLE_LABELS[role];
  const words = role.toLowerCase().replaceAll("_", " ");
  return words.charAt(0).toUpperCase() + words.slice(1);
}

export function roleDescription(role: string): string {
  return ROLE_DESCRIPTIONS[role] ?? "Rôle personnalisé.";
}

const RESOURCE_LABELS: Record<string, string> = {
  transactions: "Transactions",
  accounts: "Comptes",
  cash: "Caisse",
  reports: "Rapports",
  users: "Utilisateurs",
  roles: "Rôles",
  countries: "Pays",
  audit: "Historique",
  vehicles: "Véhicules",
  drivers: "Chauffeurs",
  training_centers: "Centres de formation",
  inquiries: "Candidatures et messages du site",
};

// Business resources first, platform administration last.
const RESOURCE_ORDER = ["transactions", "accounts", "cash", "reports", "users", "roles", "countries", "audit", "vehicles", "drivers", "training_centers", "inquiries"];

const ACTION_LABELS: Record<string, string> = {
  read: "Consulter",
  create: "Créer",
  update: "Modifier",
  approve: "Valider",
  cancel: "Annuler",
  operate: "Opérer la caisse",
  export: "Exporter",
  deactivate: "Suspendre",
  lock: "Bloquer",
  block: "Bloquer",
  suspend: "Suspendre",
  unlock: "Débloquer la connexion",
  manage: "Gérer",
  delete: "Supprimer",
};

// Resources whose permissions are guarded by the anti-escalation rule (mirrors the backend AccessPolicy).
const ADMINISTRATIVE_RESOURCES = ["users", "roles"];

// Whether the signed-in admin may hand out `permission`.
export function isGrantable(permission: string, held: string[]): boolean {
  return !ADMINISTRATIVE_RESOURCES.includes(permission.split(":")[0]) || held.includes(permission);
}

export function permissionLabel(permission: string): string {
  const [, action] = permission.split(":");
  return ACTION_LABELS[action] ?? action;
}

export interface PermissionGroup {
  resource: string;
  label: string;
  // Full permission names, e.g.
  permissions: string[];
}

// One group per resource, in display order, each with its permissions in a stable order.
export function groupPermissions(permissions: string[]): PermissionGroup[] {
  const byResource = new Map<string, string[]>();
  for (const permission of permissions) {
    const resource = permission.split(":")[0];
    byResource.set(resource, [...(byResource.get(resource) ?? []), permission]);
  }
  const rank = (resource: string) => (RESOURCE_ORDER.includes(resource) ? RESOURCE_ORDER.indexOf(resource) : RESOURCE_ORDER.length);
  const actionRank = (p: string) => Object.keys(ACTION_LABELS).indexOf(p.split(":")[1]);

  return [...byResource.entries()]
    .map(([resource, list]) => ({
      resource,
      label: RESOURCE_LABELS[resource] ?? resource,
      permissions: list.sort((a, b) => actionRank(a) - actionRank(b)),
    }))
    .sort((a, b) => rank(a.resource) - rank(b.resource));
}
