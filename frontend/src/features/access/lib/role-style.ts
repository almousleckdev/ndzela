/** Every role gets its own colour so roles are recognisable at a glance in lists, chips and the roles page. */
export interface RoleTone {
  dot: string;
  chip: string;
  ring: string;
}

const TONES: Record<string, RoleTone> = {
  orange: { dot: "bg-orange-500", chip: "bg-orange-50 text-orange-800 ring-orange-200", ring: "ring-orange-300" },
  indigo: { dot: "bg-indigo-500", chip: "bg-indigo-50 text-indigo-800 ring-indigo-200", ring: "ring-indigo-300" },
  teal: { dot: "bg-teal-500", chip: "bg-teal-50 text-teal-800 ring-teal-200", ring: "ring-teal-300" },
  slate: { dot: "bg-slate-400", chip: "bg-slate-100 text-slate-700 ring-slate-200", ring: "ring-slate-300" },
  rose: { dot: "bg-rose-500", chip: "bg-rose-50 text-rose-800 ring-rose-200", ring: "ring-rose-300" },
  emerald: { dot: "bg-emerald-500", chip: "bg-emerald-50 text-emerald-800 ring-emerald-200", ring: "ring-emerald-300" },
  amber: { dot: "bg-amber-500", chip: "bg-amber-50 text-amber-800 ring-amber-200", ring: "ring-amber-300" },
  violet: { dot: "bg-violet-500", chip: "bg-violet-50 text-violet-800 ring-violet-200", ring: "ring-violet-300" },
};

const FIXED: Record<string, keyof typeof TONES> = {
  SUPER_ADMIN: "orange",
  ADMIN: "indigo",
  GESTIONNAIRE: "teal",
  COMPTABLE: "emerald",
  CAISSIER: "amber",
  VIEWER: "slate",
};

const CUSTOM = ["rose", "violet"] as const;

export function roleTone(role: string): RoleTone {
  const fixed = FIXED[role];
  if (fixed) return TONES[fixed];
  const hash = [...role].reduce((sum, char) => sum + char.charCodeAt(0), 0);
  return TONES[CUSTOM[hash % CUSTOM.length]];
}
