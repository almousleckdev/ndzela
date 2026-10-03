import { siteConfig } from "@/config/site";
import type { LegalDocument } from "@/features/legal/types";

const { contact } = siteConfig;

export const privacy: LegalDocument = {
  title: "Politique de confidentialité",
  description: "Comment Ndzela collecte, utilise et protège vos données personnelles.",
  updated: "3 octobre 2026",
  intro: `${siteConfig.legalName} (« Ndzela ») protège vos données personnelles. Cette page explique ce que nous collectons, pourquoi, combien de temps nous le conservons et comment exercer vos droits. Elle s'applique à ce site et à l'application mobile Ndzela.`,
  sections: [
    { title: "Qui est responsable de vos données", paragraphs: [`${siteConfig.legalName}, ${contact.street}, ${contact.postalBox}, ${contact.city}, ${contact.country}. Pour toute question : ${contact.email}.`] },
    {
      title: "Les données que nous collectons",
      items: [
        "Sur le site : les informations que vous saisissez dans le formulaire de contact (nom, e-mail, téléphone, message).",
        "Pour une candidature de chauffeur : identité, coordonnées, date de naissance, adresse, numéro de permis, et les documents envoyés (photo, permis de conduire, pièce d'identité, casier judiciaire).",
        "Pour un chauffeur partenaire : les informations du contrat, le véhicule attribué, l'historique des loyers et des paiements, le numéro Mobile Money utilisé pour payer.",
        "Dans l'application : l'identifiant de connexion, et les messages envoyés au support.",
      ],
    },
    {
      title: "Pourquoi nous les utilisons",
      items: [
        "Répondre à vos messages et étudier votre candidature.",
        "Établir et gérer votre contrat de location, suivre les loyers et émettre les reçus.",
        "Traiter vos paiements Mobile Money, avec notre prestataire de paiement.",
        "Assurer la sécurité de la flotte et de l'application, et respecter nos obligations légales et comptables.",
      ],
    },
    { title: "Avec qui nous les partageons", paragraphs: ["Nous ne vendons jamais vos données. Elles sont accessibles à l'équipe Ndzela habilitée, et à nos prestataires strictement nécessaires au service : le prestataire de paiement Mobile Money (pour traiter vos paiements) et l'hébergeur de nos systèmes. Nous pouvons les communiquer aux autorités lorsque la loi l'exige."] },
    { title: "Combien de temps nous les conservons", paragraphs: ["Les candidatures et messages sont conservés le temps nécessaire à leur traitement, puis supprimés. Les données du contrat et des paiements sont conservées pendant la durée du contrat, puis pendant la durée imposée par la loi pour les documents comptables. Au-delà, elles sont supprimées ou rendues anonymes."] },
    { title: "Comment nous les protégeons", paragraphs: ["Les échanges avec nos systèmes sont chiffrés. Les documents que vous nous confiez sont stockés dans un espace privé, accessible seulement au personnel autorisé, qui s'authentifie pour y accéder. Aucun paiement n'est conservé en dehors de l'historique du loyer : nous ne stockons ni code PIN ni mot de passe Mobile Money."] },
    {
      title: "Vos droits",
      paragraphs: [`Vous pouvez demander l'accès à vos données, leur correction, leur suppression ou vous opposer à leur utilisation, en écrivant à ${contact.email}. Un chauffeur peut aussi demander la suppression de son compte depuis l'application (Profil, Mes données) ou depuis la page « Suppression de compte ».`],
    },
    { title: "Cookies", paragraphs: ["Ce site n'utilise ni cookie publicitaire ni outil de suivi. Si cela change, nous vous le demanderons d'abord."] },
    { title: "Modifications", paragraphs: ["Nous pouvons mettre à jour cette politique. La date de dernière mise à jour figure en haut de la page."] },
  ],
};
