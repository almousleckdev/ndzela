export interface RoleSummary {
  id: string;
  name: string;
  permissions: string[];
  userCount: number;
  // Built-in role (SUPER_ADMIN)
  system: boolean;
}
