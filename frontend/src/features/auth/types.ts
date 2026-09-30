export type LoginStep = "EMAIL_OTP" | "MFA_SETUP" | "MFA_VERIFY";

export interface LoginStepResponse {
  challengeToken: string;
  nextStep: LoginStep;
  /** Seconds before another OTP may be requested. */
  resendInSeconds: number;
}

export interface MfaSetupResponse {
  secret: string;
  qrCodeDataUri: string;
}

/** The refresh token is never exposed here: it lives in an HttpOnly cookie. */
export interface AuthResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresInSeconds: number;
}

/** A tenant (country). */
export interface Country {
  id: string;
  code: string;
  name: string;
  currencyCode: string;
  phoneCode: string;
  timezone: string;
  defaultLanguage: string;
}

export interface UserProfile {
  email: string;
  firstName: string | null;
  lastName: string | null;
  phoneNumber: string | null;
  profilePictureUrl: string | null;
  mfaEnabled: boolean;
  /** Bare role names, e.g. SUPER_ADMIN. */
  roles: string[];
  /** `resource:action` strings, e.g. users:read. */
  permissions: string[];
  /** Null for a global user (the super admin). */
  country: Country | null;
}
