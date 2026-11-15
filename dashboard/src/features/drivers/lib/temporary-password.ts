// Letters and digits that cannot be mistaken for one another when the password is read out loud (no 0/O, 1/l/I).
const LOWER = "abcdefghijkmnpqrstuvwxyz";
const UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
const DIGITS = "23456789";

function pick(set: string): string {
  const [random] = crypto.getRandomValues(new Uint32Array(1));
  return set[random % set.length];
}

// A readable temporary password with at least one lower-case letter, one upper-case letter and one digit.
export function generateTemporaryPassword(length = 12): string {
  const all = LOWER + UPPER + DIGITS;
  const chars = [pick(LOWER), pick(UPPER), pick(DIGITS), ...Array.from({ length: length - 3 }, () => pick(all))];
  for (let i = chars.length - 1; i > 0; i -= 1) {
    const [random] = crypto.getRandomValues(new Uint32Array(1));
    const j = random % (i + 1);
    [chars[i], chars[j]] = [chars[j], chars[i]];
  }
  return chars.join("");
}
