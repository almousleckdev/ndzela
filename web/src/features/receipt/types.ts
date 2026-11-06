export interface ReceiptDay {
  day: string;
  kind: "RENT" | "PENALTY";
  amount: number;
  onTime: boolean;
}

export interface PublicReceipt {
  reference: string;
  paidAt: string;
  operator: string;
  maskedPhone: string | null;
  payerName: string;
  contractNumber: string;
  amount: number;
  fee: number;
  total: number;
  currency: string;
  beneficiaryName: string;
  cancelled: boolean;
  days: ReceiptDay[];
  creditKept: number;
}
