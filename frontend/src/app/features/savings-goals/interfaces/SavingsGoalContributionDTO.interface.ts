/** Body for deposit / withdraw. */
export interface SavingsGoalContributionDTO {
  accountId: string;
  amount: number;
  /** ISO local date-time; defaults to now on the server. */
  date?: string | null;
  note?: string | null;
}

/** What happens with the money a goal still holds when it is closed. */
export type SavingsGoalCloseOutcome = 'RETURN' | 'SPEND';

/** Body for close; `accountId` may be omitted when the goal is already empty. */
export interface SavingsGoalCloseDTO {
  accountId?: string | null;
  /** Defaults to `RETURN` on the server. */
  outcome?: SavingsGoalCloseOutcome | null;
  /** Optional category for the expense recorded by `SPEND`. */
  categoryId?: string | null;
}
