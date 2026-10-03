import { siteConfig } from "@/config/site";
import type { LegalDocument } from "@/features/legal/types";

const { contact } = siteConfig;

export const accountDeletion: LegalDocument = {
  title: "Suppression de compte",
  description: "Comment demander la suppression de votre compte Ndzela et de vos données.",
  updated: "3 octobre 2026",
  intro: "Vous pouvez demander à tout moment la suppression de votre compte et de vos données personnelles. Voici comment faire et ce qui se passe ensuite.",
  sections: [
    { title: "Depuis l'application", paragraphs: ["Ouvrez l'application, allez dans Profil, puis Mes données, et touchez « Supprimer mon compte ». Confirmez : votre demande est transmise à l'équipe Ndzela."] },
    { title: "Sans l'application", paragraphs: [`Écrivez-nous à ${contact.email} depuis l'adresse e-mail liée à votre compte, ou appelez le ${contact.phone} (aussi sur WhatsApp au ${siteConfig.whatsapp.display}). Indiquez votre nom et votre numéro de téléphone pour que nous retrouvions votre compte.`] },
    {
      title: "Ce qui est supprimé",
      items: ["Votre accès à l'application.", "Vos informations personnelles : coordonnées, photo, documents envoyés (permis, pièce d'identité, casier judiciaire).", "Vos messages au support."],
    },
    { title: "Ce qui peut être conservé", paragraphs: ["L'historique de votre contrat et de vos paiements de loyer est conservé aussi longtemps que la loi nous l'impose (obligations comptables et fiscales), puis supprimé. Un contrat en cours doit d'abord être terminé et les loyers dus réglés."] },
    { title: "Délai", paragraphs: ["Nous vous répondons pour confirmer la suppression, et nous vous disons ce qui est conservé et pourquoi."] },
    { title: "Candidats et visiteurs", paragraphs: [`Si vous avez envoyé une candidature ou un message depuis le site, écrivez-nous à ${contact.email} pour demander leur suppression.`] },
  ],
};
