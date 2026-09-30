import { AuthShell } from "@/features/auth/components/auth-shell";
import { LoginFlow } from "@/features/auth/components/login-flow";

export default function LoginPage() {
  return (
    <AuthShell>
      <LoginFlow />
    </AuthShell>
  );
}
