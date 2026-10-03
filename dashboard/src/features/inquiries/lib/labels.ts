import type { Tone } from "@/components/data-table/cells";
import type { ApplicationStatus, MessageStatus } from "@/features/inquiries/types";

export const APPLICATION_STATUS_LABELS: Record<ApplicationStatus, string> = { NEW: "Nouvelle", CONTACTED: "Contacté", ACCEPTED: "Acceptée", REJECTED: "Refusée" };
export const APPLICATION_STATUS_TONE: Record<ApplicationStatus, Tone> = { NEW: "info", CONTACTED: "watch", ACCEPTED: "good", REJECTED: "bad" };
export const MESSAGE_STATUS_LABELS: Record<MessageStatus, string> = { NEW: "Nouveau", HANDLED: "Traité" };
export const MESSAGE_STATUS_TONE: Record<MessageStatus, Tone> = { NEW: "info", HANDLED: "good" };
