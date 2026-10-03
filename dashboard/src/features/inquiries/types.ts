export type ApplicationStatus = "NEW" | "CONTACTED" | "ACCEPTED" | "REJECTED";
export type MessageStatus = "NEW" | "HANDLED";
export type ApplicationFileKind = "photo" | "licence" | "id" | "address-proof";

// A driver application sent from the public website.
export interface Application {
  id: string;
  firstName: string;
  lastName: string;
  phone: string;
  email: string | null;
  birthDate: string | null;
  city: string | null;
  address: string | null;
  licenceNumber: string | null;
  message: string | null;
  hasAddressProof: boolean;
  status: ApplicationStatus;
  staffNote: string | null;
  interviewAt: string | null;
  candidateMessage: string | null;
  // True when the candidate left an e-mail, so the answer is also sent by e-mail.
  notified: boolean;
  reviewedBy: string | null;
  reviewedAt: string | null;
  createdAt: string;
}

// A message sent from the contact page of the public website.
export interface ContactMessage {
  id: string;
  name: string;
  email: string | null;
  phone: string | null;
  subject: string;
  message: string;
  status: MessageStatus;
  handledBy: string | null;
  handledAt: string | null;
  createdAt: string;
}

// The staff's follow-up
export interface ApplicationDecision {
  status: ApplicationStatus;
  note?: string;
  interviewAt?: string;
  candidateMessage?: string;
}
