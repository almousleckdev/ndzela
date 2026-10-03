export interface LegalSection {
  title: string;
  paragraphs?: string[];
  items?: string[];
}

export interface LegalDocument {
  title: string;
  description: string;
  // Shown as "Dernière mise à jour".
  updated: string;
  intro: string;
  sections: LegalSection[];
}
