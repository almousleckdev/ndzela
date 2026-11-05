import { joinPhone, splitPhone } from "@/lib/phone";
import { DEFAULT_DEPOSIT } from "@/features/contracts/schemas";
import type { DriverFormValues } from "@/features/drivers/schemas/driver";
import type { Driver } from "@/features/drivers/types";

// The dialing code chosen for each phone number of the form.
export interface PhoneCodes {
  primary: string;
  secondary: string;
  emergency: string;
}

// The files picked in the wizard
export type UploadKey = "photo" | "licence" | "id" | "addressProof" | "residenceCertificate";
export type Uploads = Record<UploadKey, File | null>;
export interface PhotoPick {
  file: File | null;
  error?: string;
  onChange: (file: File | null, error?: string) => void;
}

export const NO_UPLOADS: Uploads = { photo: null, licence: null, id: null, addressProof: null, residenceCertificate: null };

export const EMPTY_DRIVER: DriverFormValues = {
  personal: {
    civility: "" as DriverFormValues["personal"]["civility"],
    lastName: "", firstName: "", birthDate: "", birthPlace: "", nationality: "",
    idType: "" as DriverFormValues["personal"]["idType"],
    idNumber: "", idIssueDate: "", idIssuePlace: "", address: "", district: "", city: "",
    phonePrimary: "", phoneSecondary: "", email: "", licenceNumber: "", licenceIssueDate: "",
    licenceCategory: "" as DriverFormValues["personal"]["licenceCategory"],
  },
  emergency: {
    lastName: "", firstName: "", phone: "",
    relationship: "" as DriverFormValues["emergency"]["relationship"],
    address: "",
  },
  training: { trainingCenterId: "", trainingDate: "", certificateNumber: "" },
  vehicleId: "",
  contract: { deposit: DEFAULT_DEPOSIT },
};

// The form values (phones split into code and number) for editing an existing driver.
export function valuesFromDriver(driver: Driver, knownCodes: string[]): { values: DriverFormValues; codes: Partial<PhoneCodes> } {
  const primary = splitPhone(driver.personal.phonePrimary, knownCodes);
  const secondary = splitPhone(driver.personal.phoneSecondary, knownCodes);
  const emergency = splitPhone(driver.emergency.phone, knownCodes);
  return {
    values: {
      personal: { ...driver.personal, phonePrimary: primary.number, phoneSecondary: secondary.number },
      emergency: { ...driver.emergency, phone: emergency.number },
      training: {
        trainingCenterId: driver.training.trainingCenter?.id ?? "",
        trainingDate: driver.training.trainingDate,
        certificateNumber: driver.training.certificateNumber ?? "",
      },
      vehicleId: "",
      contract: { deposit: driver.contract ? String(driver.contract.deposit) : DEFAULT_DEPOSIT },
    },
    codes: { primary: primary.code ?? undefined, secondary: secondary.code ?? undefined, emergency: emergency.code ?? undefined },
  };
}

// The request body
export function toRequestBody(values: DriverFormValues, codes: PhoneCodes, extra?: { vehicleId: string; password: string }) {
  const secondary = joinPhone(codes.secondary, values.personal.phoneSecondary ?? "");
  return {
    personal: {
      ...values.personal,
      phonePrimary: joinPhone(codes.primary, values.personal.phonePrimary),
      phoneSecondary: secondary || null,
    },
    emergency: { ...values.emergency, phone: joinPhone(codes.emergency, values.emergency.phone) },
    training: { ...values.training, trainingCenterId: values.training.trainingCenterId || null, certificateNumber: values.training.certificateNumber.trim() || null },
    ...(extra ? { vehicleId: extra.vehicleId, contract: { deposit: Number(values.contract.deposit) }, password: extra.password } : {}),
  };
}
