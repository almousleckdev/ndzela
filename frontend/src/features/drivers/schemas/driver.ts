import { z } from "zod";
import { nationalNumberSchema } from "@/lib/phone";
import { depositSchema } from "@/features/contracts/schemas";

const today = () => new Date().toISOString().slice(0, 10);
const text = (max = 100) => z.string().trim().min(1, "Champ obligatoire").max(max, `${max} caractères maximum`);
const pastDate = () =>
  z.string().min(1, "Date obligatoire").refine((value) => value <= today(), "La date ne peut pas être dans le futur");
const requiredNumber = nationalNumberSchema.refine((value) => value.trim() !== "", "Le numéro est requis");

function isAdult(birthDate: string): boolean {
  const limit = new Date();
  limit.setFullYear(limit.getFullYear() - 18);
  return birthDate <= limit.toISOString().slice(0, 10);
}

export const driverSchema = z.object({
  personal: z.object({
    civility: z.enum(["MALE", "FEMALE"], { message: "Choisissez la civilité" }),
    lastName: text(),
    firstName: text(),
    birthDate: z.string().min(1, "Date obligatoire").refine(isAdult, "Le chauffeur doit avoir au moins 18 ans"),
    birthPlace: text(),
    nationality: text(60),
    idType: z.enum(["NATIONAL_ID", "PASSPORT"], { message: "Choisissez le type de pièce" }),
    idNumber: text(50),
    idIssueDate: pastDate(),
    idIssuePlace: text(),
    address: text(255),
    district: text(),
    city: text(),
    phonePrimary: requiredNumber,
    phoneSecondary: nationalNumberSchema,
    email: z.email("Adresse email invalide"),
    licenceNumber: text(50),
    licenceIssueDate: pastDate(),
    licenceCategory: z.enum(["A", "B", "C", "D"], { message: "Choisissez la catégorie" }),
  }),
  emergency: z.object({
    lastName: text(),
    firstName: text(),
    phone: requiredNumber,
    relationship: z.enum(["FATHER", "MOTHER", "BROTHER", "SPOUSE", "SISTER", "UNCLE", "AUNT", "COUSIN", "FRIEND", "OTHER"], { message: "Choisissez le lien de parenté" }),
    address: text(255),
  }),
  training: z.object({
    trainingCenterId: z.string().min(1, "Choisissez un centre de formation"),
    trainingDate: pastDate(),
    certificateNumber: z.string().trim().max(50, "50 caractères maximum"),
  }),
  vehicleId: z.string(),
  contract: z.object({ deposit: depositSchema }),
});

export type DriverFormValues = z.infer<typeof driverSchema>;

export const passwordSchema = z
  .object({
    password: z.string().min(8, "Le mot de passe doit contenir au moins 8 caractères").max(72, "72 caractères maximum"),
    confirmation: z.string(),
  })
  .refine((values) => values.password === values.confirmation, { path: ["confirmation"], message: "Les mots de passe ne correspondent pas" });
export type PasswordValues = z.infer<typeof passwordSchema>;
