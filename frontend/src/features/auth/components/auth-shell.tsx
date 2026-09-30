import Image from "next/image";

/** Full-screen background with the translucent card that hosts every unauthenticated screen. */
export function AuthShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative flex min-h-screen w-full items-center justify-end p-6 lg:p-12 xl:pr-32">
      <div
        className="absolute inset-0 z-0 bg-cover bg-center bg-no-repeat"
        style={{ backgroundImage: 'url("/images/image.png")' }}
      >
        <div className="absolute inset-0 bg-slate-900/30" />
      </div>

      <main className="relative z-10 w-full max-w-md rounded-3xl border border-white/40 bg-white/60 p-8 shadow-2xl backdrop-blur-md sm:p-10">
        <Image src="/images/logo.png" alt="NDZela Admin" width={80} height={80} priority className="mx-auto mb-8 h-20 w-auto" />
        {children}
      </main>
    </div>
  );
}
