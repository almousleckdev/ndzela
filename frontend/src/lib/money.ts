/** 23000 becomes "23 000 FCFA". */
export function formatFcfa(amount: number): string {
  // The French grouping space is a narrow no-break space that most fonts draw too tight; a plain no-break space reads better.
  return `${new Intl.NumberFormat("fr-FR").format(amount).replace(/\u202f/g, "\u00a0")}\u00a0FCFA`;
}
