/** Body for deposit / withdraw. */
export interface SavingsGoalContributionDTO {
  accountId: string;
  amount: number;
  /** ISO local date-time; defaults to now on the server. */
  date?: string | null;
  note?: string | null;
}

/** Body for close; `accountId` may be omitted when the goal is already empty. */
export interface SavingsGoalCloseDTO {
  accountId?: string | null;
}
