import { Container } from "@/components/ui/container";
import { RoadLine } from "@/components/ui/road-line";
import type { LegalDocument } from "@/features/legal/types";

// A legal text
export function LegalPage({ document }: { document: LegalDocument }) {
  return (
    <>
      <section className="bg-sky py-14 sm:py-16">
        <Container className="max-w-3xl">
          <h1 className="font-display text-4xl font-bold tracking-tight text-balance sm:text-5xl">{document.title}</h1>
          <p className="mt-4 text-sm text-muted">Dernière mise à jour : {document.updated}</p>
          <RoadLine className="mt-8 max-w-xs" />
        </Container>
      </section>
      <section className="py-14 sm:py-16">
        <Container className="max-w-3xl">
          <p className="text-lg leading-relaxed text-muted">{document.intro}</p>
          <div className="mt-10 space-y-10">
            {document.sections.map((section, index) => (
              <section key={section.title} aria-labelledby={`legal-${index}`}>
                <h2 id={`legal-${index}`} className="font-display text-2xl font-bold">{index + 1}. {section.title}</h2>
                {section.paragraphs?.map((text) => <p key={text} className="mt-3 leading-relaxed">{text}</p>)}
                {section.items && (
                  <ul className="mt-3 list-disc space-y-1.5 pl-6 leading-relaxed marker:text-primary">
                    {section.items.map((item) => <li key={item}>{item}</li>)}
                  </ul>
                )}
              </section>
            ))}
          </div>
        </Container>
      </section>
    </>
  );
}
