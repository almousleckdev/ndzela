export type LoginStep = "EMAIL_OTP" | "MFA_SETUP" | "MFA_VERIFY";

export interface LoginStepResponse {
  challengeToken: string;
  nextStep: LoginStep;
  // Seconds before another OTP may be requested.
  resendInSeconds: number;
}

export interface MfaSetupResponse {
  secret: string;
  qrCodeDataUri: string;
}

// The refresh token is never exposed here
export interface AuthResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresInSeconds: number;
}

// A tenant (country).
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
  // Bare role names, e.g.
  roles: string[];
  // `resource:action` strings, e.g.
  permissions: string[];
  // Sees every country, not only the ones in `countries` (which is then empty).
  isGlobal: boolean;
  // The countries this user may work in. Empty when global.
  countries: Country[];
  // Default/home country: where the switcher starts. Null for a global user.
  country: Country | null;
}
