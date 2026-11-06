"use client";

interface AttemptsNoticeProps {
  failed: number;
  pending: number;
  status: string;
  onShow: (status: string) => void;
}

// The attempts that did not become a payment, counted so they do not clutter the list but are never hidden
export function AttemptsNotice({ failed, pending, status, onShow }: AttemptsNoticeProps) {
  if (status !== "" || (failed === 0 && pending === 0)) return null;
  const button = "font-medium text-primary hover:underline";
  return (
    <p className="px-4 pb-2 text-sm text-muted">
      {failed > 0 && <><button type="button" className={button} onClick={() => onShow("FAILED")}>{failed} tentative{failed > 1 ? "s" : ""} échouée{failed > 1 ? "s" : ""}</button> sur cette période</>}
      {failed > 0 && pending > 0 && " · "}
      {pending > 0 && <button type="button" className={button} onClick={() => onShow("PENDING")}>{pending} en attente</button>}
    </p>
  );
}
