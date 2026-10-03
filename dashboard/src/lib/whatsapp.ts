// A WhatsApp chat link for a phone number (digits only, with the country code)
export function whatsappLink(phone: string, message?: string): string {
  const digits = phone.replace(/\D/g, "");
  return `https://wa.me/${digits}${message ? `?text=${encodeURIComponent(message)}` : ""}`;
}
