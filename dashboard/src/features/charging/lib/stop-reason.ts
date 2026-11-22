const DRIVER_STOPPED = "Arrêt par le conducteur";
const AMOUNT_REACHED = "Montant atteint";
const POWER_CUT = "Coupure d'électricité";
const REASON: Record<string, string> = {
  Remote: DRIVER_STOPPED, Local: DRIVER_STOPPED, EVDisconnected: DRIVER_STOPPED, EmergencyStop: DRIVER_STOPPED, DeAuthorized: DRIVER_STOPPED, UnlockCommand: DRIVER_STOPPED,
  Other: AMOUNT_REACHED, PowerLoss: POWER_CUT, Reboot: POWER_CUT, HardReset: POWER_CUT, SoftReset: POWER_CUT,
  FREE_WINDOW_ENDED: "Fin du créneau gratuit (22h)",
};

export const reasonLabel = (reason: string | null, finished = true) => (reason && REASON[reason]) || (finished ? AMOUNT_REACHED : null);
