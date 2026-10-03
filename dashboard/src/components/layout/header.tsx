"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { LogOut, Menu, Settings, User as UserIcon } from "lucide-react";
import { useShellStore } from "@/features/layout/shell-store";
import { NotificationBell } from "@/features/notifications/components/notification-bell";
import { CountrySwitch } from "@/features/tenancy/components/country-switch";
import { Avatar } from "@/components/ui/avatar";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Button } from "@/components/ui/button";
import { Dropdown, DropdownItem } from "@/components/ui/dropdown";
import { displayName } from "@/features/profile/lib/user";
import { authService } from "@/features/auth/services/auth.service";
import { useAuthStore } from "@/features/auth/store/auth.store";

export function Header() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const setMobileOpen = useShellStore((state) => state.setMobileOpen);
  const [isLogoutOpen, setIsLogoutOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const handleLogout = async () => {
    setIsLoggingOut(true);
    await authService.logout().catch(() => undefined);
    router.replace("/login");
  };

  return (
    <>
      <header className="flex h-16 shrink-0 items-center justify-between gap-3 border-b border-line bg-white px-4 sm:px-6">
        <div className="flex min-w-0 items-center gap-2">
          <button type="button" aria-label="Ouvrir le menu" onClick={() => setMobileOpen(true)} className="-ml-1 rounded-md p-2 text-muted hover:bg-slate-100 hover:text-ink lg:hidden"><Menu className="h-5 w-5" /></button>
          <CountrySwitch />
        </div>

        <div className="flex items-center gap-3">
          <NotificationBell />

          <Dropdown
            trigger={
              <Button variant="ghost" aria-label="Menu du profil" className="h-auto rounded-full p-0">
                <Avatar name={user ? displayName(user) : ""} src={user?.profilePictureUrl} size="md" />
              </Button>
            }
          >
            <DropdownItem onClick={() => router.push("/profile")}>
              <span className="flex items-center"><UserIcon className="mr-2 h-4 w-4" /> Profil</span>
            </DropdownItem>
            <DropdownItem onClick={() => router.push("/settings")}>
              <span className="flex items-center"><Settings className="mr-2 h-4 w-4" /> Paramètres</span>
            </DropdownItem>
            <div className="my-1 border-t border-slate-100" />
            <DropdownItem destructive onClick={() => setIsLogoutOpen(true)}>
              <span className="flex items-center"><LogOut className="mr-2 h-4 w-4" /> Déconnexion</span>
            </DropdownItem>
          </Dropdown>
        </div>
      </header>

      <ConfirmDialog
        isOpen={isLogoutOpen}
        onClose={() => setIsLogoutOpen(false)}
        onConfirm={handleLogout}
        isLoading={isLoggingOut}
        title="Confirmer la déconnexion"
        description="Voulez-vous vraiment vous déconnecter de NDZela Admin ?"
        confirmText="Se déconnecter"
        cancelText="Annuler"
        destructive
      />
    </>
  );
}
