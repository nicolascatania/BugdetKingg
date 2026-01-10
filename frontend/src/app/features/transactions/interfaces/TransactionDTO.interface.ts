export interface TransactionDTO {
    id: string;
    account: string;
    amount: number;
    date: string;
    description: string;
    category: string;
    categoryName: string;
    type: string;
    counterparty: string;
    destinationAccount?: string;
    accountName?: string;
}