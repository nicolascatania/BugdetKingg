export interface TransactionDTO {
    id: string;
    account: string;
    amount: number;
    date: string;
    description: string;
    category: string;
    type: string;
    counterparty: string;
    destinationAccount?: string;
}