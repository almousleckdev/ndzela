import Image from "next/image";

// Every unauthenticated screen: the fleet picture on the left (large screens), the form on the right.
export function AuthShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid min-h-screen w-full bg-white lg:grid-cols-2">
      <aside className="relative hidden overflow-hidden bg-slate-100 lg:block">
        <Image src="/images/img2.png" alt="" fill priority sizes="50vw" className="object-cover object-center" />
        <div className="absolute inset-0 bg-gradient-to-t from-slate-900/75 via-slate-900/10 to-transparent" />
        <Image src="/images/logo.png" alt="NDZela Admin" width={80} height={80} priority className="absolute left-10 top-10 h-14 w-auto drop-shadow-md" />
        <div className="absolute inset-x-0 bottom-0 px-10 pb-8 text-white">
          <p className="text-3xl font-semibold leading-tight">Pilotez votre flotte,<br />simplement.</p>
          <p className="mt-3 max-w-md text-sm text-white/80">Véhicules, chauffeurs, contrats et paiements : toute l&apos;exploitation NDZela au même endroit.</p>
        </div>
      </aside>

      <main className="flex items-center justify-center px-6 py-12 sm:px-12">
        <div className="w-full max-w-sm [&_input]:h-11">
          {/* On small screens the picture panel is hidden, so the logo stays above the form. */}
          <Image src="/images/logo.png" alt="NDZela Admin" width={80} height={80} priority className="mb-10 h-12 w-auto lg:hidden" />
          {children}
        </div>
      </main>
    </div>
  );
}
