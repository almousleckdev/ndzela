export const driver = {
  id: "driver-1",
  personal: {
    civility: "MALE", lastName: "Khatim", firstName: "Rachid Cedric", birthDate: "1979-08-11", birthPlace: "Bitam", nationality: "Gabonaise",
    idType: "NATIONAL_ID", idNumber: "22PP11614", idIssueDate: "2022-10-13", idIssuePlace: "Libreville", address: "Okala", district: "Okala", city: "Libreville",
    phonePrimary: "+241 62143500", phoneSecondary: null, email: "rachid@ndzela.test", licenceNumber: "20191B03151", licenceIssueDate: "2019-03-09", licenceCategory: "B",
  },
  emergency: { lastName: "Nang Ndong", firstName: "Brigitte", phone: "+241 66685807", relationship: "MOTHER", address: "Akanda" },
  training: { trainingCenter: { id: "center-1", name: "NDZELA" }, trainingDate: "2026-10-15", certificateNumber: null },
  status: "ACTIVE", onLeave: false, statusReason: null,
  files: { photo: false, licence: false, id: false, addressProof: false },
  mustChangePassword: false,
  vehicle: { id: "vehicle-1", fleetNumber: "V-29", brand: "Dongfeng", model: "Box", year: 2025, plate: "LZ-101-AA", dailyRent: 23000, status: "ASSIGNED" },
  contract: {
    id: "contract-1", contractNumber: "NDZ-60749194", status: "ACTIVE", driverId: "driver-1", vehicleId: "vehicle-1", startDate: "2026-10-15", endDate: "2030-10-10",
    billableDays: 1248, dailyRent: 23000, deposit: 500000, totalPrice: 28704000, revision: 0, updatedAt: null, createdAt: "2026-10-15T08:00:00Z", endedAt: null, endReason: null,
  },
  arrears: { level: "YELLOW", overdueAmount: 69000, overdueSince: null, unpaidDays: 3, paidDays: 9, daysLeft: 1236 },
  country: null, loginLocked: false, createdAt: "2026-10-15T08:00:00Z",
};

export const account = {
  driverId: "driver-1", driverName: "Khatim Rachid Cedric", driverStatus: "ACTIVE", contractId: "contract-1", contractNumber: "NDZ-60749194", dailyRent: 23000,
  level: "YELLOW", overdueAmount: 69000, credit: 0, overdueSince: null, paidOnTime: 8, paidLate: 1, unpaid: 3, days: [], entries: [],
  weekClosesAt: "2026-10-25T12:00:00+01:00", weekSettled: false,
  progress: { totalDays: 1248, elapsedDays: 12, daysLeft: 1236, paidDays: 9, unpaidDays: 3, totalPrice: 28704000, paidAmount: 207000, remainingAmount: 28497000, deposit: 500000 },
};

export const operators = [
  { code: "AIRTEL_MONEY", label: "Airtel Money" },
  { code: "MOOV_MONEY", label: "Moov Money" },
];
