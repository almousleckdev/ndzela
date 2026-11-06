# ADR-0029: Rent is due the same day at noon and can be paid from midnight

**Status:** Accepted · **Date:** 2026-11-06

**Context.** The rent of day D was due D+1 at 12:00. A driver whose contract was signed on 04/11 saw nothing to pay on 05/11 and only one unpaid day on 06/11, and could not pay a day before its deadline. The product owner wants the deadline on the day itself.

**Decision.** The first rent day is the day after the contract is signed (the signing day is not billed). The rent of day D (Monday to Saturday) is **open from D 00:00** and **due D 12:00**; after noon without a covering payment it is **unpaid**. An open day is shown as *awaiting* and can be paid early: the payable options are the unpaid days oldest first, then the open day. The 1-2 / 3 rule counts the **days to settle** (unpaid plus the open day): with 1 or 2 the driver pays the oldest or both together; from 3 he cannot choose and the payment screen offers all of them at once. The level and the suspension still follow the unpaid days only. The payment week is unchanged: Monday starts a week, it must be settled before **Sunday 12:00**, the last warning. The weekly history lists Monday to Saturday with paid, paid late, unpaid and awaiting days, and the days still to come greyed out.

**Consequences.** (+) The rule matches how the owner describes it and a good driver can pay every morning. (-) Arrears levels and the automatic suspension now trigger a day earlier than before (3 unpaid days at noon of the third day, not the fourth). Contracts already running are recalculated at once because nothing is stored per day. The legacy-import parity note (docs/migration/legacy-import.md) was written for the old clock.
