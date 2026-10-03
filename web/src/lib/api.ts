const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const MESSAGES: Record<string, string> = {
  TOO_MANY_REQUESTS: "Vous avez envoyé trop de demandes. Réessayez dans une heure.",
  DOCUMENTS_REQUIRED: "La photo, le permis de conduire et la pièce d'identité sont obligatoires.",
  INVALID_FILE: "Un fichier n'est pas accepté : utilisez une image (JPG, PNG, WebP) ou un PDF.",
  FILE_TOO_LARGE: "Un fichier dépasse 5 Mo.",
};

// A failed submission with a message that can be shown to the visitor as it is.
export class SubmitError extends Error {}

// Sends a JSON object or a multipart form to the backend's public endpoints; resolves when it was accepted.
export async function submitToApi(path: string, body: object | FormData): Promise<void> {
  const isForm = body instanceof FormData;
  let response: Response;
  try {
    response = await fetch(`${API_URL}/api/v1/public${path}`, {
      method: "POST",
      headers: isForm ? undefined : { "Content-Type": "application/json" },
      body: isForm ? body : JSON.stringify(body),
    });
  } catch {
    throw new SubmitError("Impossible de joindre le serveur. Vérifiez votre connexion et réessayez.");
  }
  if (response.ok) return;
  const error = (await response.json().catch(() => null)) as { code?: string } | null;
  throw new SubmitError((error?.code && MESSAGES[error.code]) || "L'envoi a échoué. Vérifiez les informations saisies et réessayez.");
}
