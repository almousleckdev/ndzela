import type { DriverFormValues } from "@/features/drivers/schemas/driver";
import type { Driver } from "@/features/drivers/types";

// A complete, valid driver form (national phone numbers, no dialling code).
export const validDriverValues: DriverFormValues = {
  personal: {
    civility: "MALE", lastName: "Khatim", firstName: "Rachid Cedric", birthDate: "1979-08-11", birthPlace: "Bitam", nationality: "Gabonaise",
    idType: "NATIONAL_ID", idNumber: "22PP11614", idIssueDate: "2022-10-13", idIssuePlace: "Libreville", address: "Okala, derrière Ciciba",
    district: "Okala", city: "Libreville", phonePrimary: "62143500", phoneSecondary: "", email: "rachid@ndzela.test",
    licenceNumber: "20191B03151", licenceIssueDate: "2019-03-09", licenceCategory: "B",
  },
  emergency: { lastName: "Nang Ndong", firstName: "Brigitte", phone: "66685807", relationship: "MOTHER", address: "Akanda, Ciciba" },
  training: { trainingCenterId: "center-1", trainingDate: "2026-10-15", certificateNumber: "" },
  vehicleId: "vehicle-1",
  contract: { deposit: "500000" },
};

// The same driver as the API returns it, with dialling codes in the stored numbers.
export const storedDriver = {
  id: "driver-1",
  personal: { ...validDriverValues.personal, phonePrimary: "+241 62143500", phoneSecondary: "+241 76290216" },
  emergency: { ...validDriverValues.emergency, phone: "+241 66685807" },
  training: { trainingCenter: { id: "center-1", name: "NDZELA" }, trainingDate: "2026-10-15", certificateNumber: null },
  status: "ACTIVE", onLeave: false, statusReason: null,
  files: { photo: true, licence: true, id: true, addressProof: false },
  mustChangePassword: false, vehicle: null,
  contract: { deposit: 500000 },
  arrears: { level: "NONE", overdueAmount: 0, overdueSince: null, unpaidDays: 0, paidDays: 0, daysLeft: 1248 },
  country: null, loginLocked: false, createdAt: "2026-10-15T08:00:00Z",
} as unknown as Driver;
