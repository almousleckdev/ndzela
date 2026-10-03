import { siteConfig } from "@/config/site";
import type { LegalDocument } from "@/features/legal/types";

const { contact } = siteConfig;

export const terms: LegalDocument = {
  title: "Conditions d'utilisation",
  description: "Les règles d'utilisation du site et de l'application mobile Ndzela.",
  updated: "3 octobre 2026",
  intro: `Ces conditions encadrent l'utilisation du site et de l'application mobile de ${siteConfig.legalName}. En les utilisant, vous les acceptez. Le contrat de location signé avec chaque chauffeur reste le document qui fixe les droits et obligations de la location.`,
  sections: [
    { title: "Le site", paragraphs: ["Le site présente Ndzela, ses véhicules électriques et sa station de recharge, et permet d'envoyer un message ou une candidature. Les informations qu'il contient sont données à titre indicatif."] },
    { title: "Candidature de chauffeur", paragraphs: ["Envoyer une candidature ne crée aucun contrat. Ndzela étudie le dossier et répond au candidat. Vous garantissez que les informations et les documents envoyés sont exacts et vous appartiennent."] },
    { title: "L'application mobile", paragraphs: ["L'application est réservée aux chauffeurs partenaires de Ndzela, dont le compte est créé par Ndzela. Vous gardez votre mot de passe confidentiel et vous êtes responsable de l'usage de votre compte."] },
    { title: "Paiement du loyer", paragraphs: ["Le loyer se règle par Mobile Money depuis l'application, avec les opérateurs proposés dans votre pays. Un reçu est disponible pour chaque paiement confirmé. Les frais de l'opérateur peuvent s'ajouter au montant du loyer et sont affichés avant la validation."] },
    { title: "Usage interdit", items: ["Utiliser le site ou l'application de manière frauduleuse ou pour nuire à un tiers.", "Tenter d'accéder à des données ou à des comptes qui ne sont pas les vôtres.", "Envoyer des documents falsifiés ou appartenant à quelqu'un d'autre."] },
    { title: "Responsabilité", paragraphs: ["Ndzela fait le nécessaire pour que le site et l'application fonctionnent normalement, sans garantir l'absence d'interruption, notamment en cas de panne du réseau ou d'un opérateur Mobile Money."] },
    { title: "Droit applicable", paragraphs: [`Ces conditions sont régies par le droit gabonais. Pour toute question : ${contact.email}.`] },
  ],
};
