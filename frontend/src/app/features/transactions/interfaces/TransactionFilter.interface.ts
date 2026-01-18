import { BaseFilter } from "../../../core/interfaces/GenericFilter.interfaces";

export interface TransactionFilter extends BaseFilter {
    dateFrom?: string;
    dateTo?: string;
    minAmount?: number;
    maxAmount?: number;
    account?: string;
    category?: string;
    type?: string;
    description?: string;
    counterparty?: string;
}