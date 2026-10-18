import type { Country } from "@/features/auth/types";
import type { Contract } from "@/features/contracts/types";
import type { Vehicle } from "@/features/vehicles/types";

export type DriverStatus = "ACTIVE" | "SUSPENDED" | "BLOCKED" | "INACTIVE";
export type Civility = "MALE" | "FEMALE";
export type IdType = "NATIONAL_ID" | "PASSPORT";
export type LicenceCategory = "A" | "B" | "C" | "D";
export type Relationship = "FATHER" | "MOTHER" | "BROTHER" | "SPOUSE" | "SISTER" | "UNCLE" | "AUNT" | "COUSIN" | "FRIEND" | "OTHER";

export interface PersonalInfo {
  civility: Civility;
  lastName: string;
  firstName: string;
  birthDate: string;
  birthPlace: string;
  nationality: string;
  idType: IdType;
  idNumber: string;
  idIssueDate: string;
  idIssuePlace: string;
  address: string;
  district: string;
  city: string;
  phonePrimary: string;
  phoneSecondary: string | null;
  email: string;
  licenceNumber: string;
  licenceIssueDate: string;
  licenceCategory: LicenceCategory;
}

export interface EmergencyContact {
  lastName: string;
  firstName: string;
  phone: string;
  relationship: Relationship;
  address: string;
}

export interface TrainingCenter {
  id: string;
  name: string;
}

export interface TrainingView {
  trainingCenter: TrainingCenter;
  trainingDate: string;
  certificateNumber: string | null;
}

export type DriverFileKind = "photo" | "licence" | "id" | "address-proof";

export interface DriverFiles {
  photo: boolean;
  licence: boolean;
  id: boolean;
  addressProof: boolean;
}

export interface Driver {
  id: string;
  personal: PersonalInfo;
  emergency: EmergencyContact;
  training: TrainingView;
  status: DriverStatus;
  onLeave: boolean;
  statusReason: string | null;
  files: DriverFiles;
  mustChangePassword: boolean;
  vehicle: Vehicle | null;
  contract: Contract | null;
  arrears: { level: "NONE" | "LATE" | "YELLOW" | "DARK_YELLOW" | "RED"; overdueAmount: number; overdueSince: string | null; unpaidDays: number; paidDays: number; daysLeft: number };
  country: Country | null;
  // Locked out of logging in by repeated failed attempts; cleared by drivers:unlock, separate from `status`.
  loginLocked: boolean;
  createdAt: string;
}

export interface DriverStats {
  total: number;
  active: number;
  onLeave: number;
  suspended: number;
  blocked: number;
  inactive: number;
  assigned: number;
  withoutVehicle: number;
  late: number;
}

export type LeaveType = "SINGLE_DAY" | "DATE_RANGE";
export type LeaveReason = "VEHICLE_BREAKDOWN" | "ACCIDENT" | "MAINTENANCE" | "PUBLIC_HOLIDAY" | "WEATHER" | "ADMINISTRATIVE" | "PERSONAL" | "OTHER";

export interface Leave {
  id: string;
  type: LeaveType;
  startDate: string;
  endDate: string;
  reason: LeaveReason;
  note: string | null;
  declaredBy: string;
  createdAt: string;
  cancelledAt: string | null;
  cancelledBy: string | null;
  state: "UPCOMING" | "ONGOING" | "PAST" | "CANCELLED";
}

export interface LeaveInput {
  type: LeaveType;
  startDate: string;
  endDate?: string;
  reason: LeaveReason;
  note?: string;
}

export interface DriverQuery {
  status: DriverStatus | "ALL";
  // CURRENT: riders who are not archived, FORMER: the archived ones
  scope?: "CURRENT" | "FORMER";
  // true
  onLeave?: boolean;
  // true
  late?: boolean;
  q: string;
  from?: string;
  to?: string;
  sort?: string;
  page: number;
  size: number;
}
