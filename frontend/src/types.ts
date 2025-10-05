// types.ts
export type Status = 'OPEN' | 'PARTIAL_PAID' | 'PAID';

export type AgingBucket = '0-7' | '8-30' | '31-60' | '61-90' | '>90';

export interface InvoiceView {
    id: number;
    qboId: string | null;
    customerName: string;
    status: Status | string;
    totalAmt: number;
    balance: number;
    txnDate: string;
    dueDate: string;
    overdue: boolean;

    daysUntilDue?: number;
    bucket?: AgingBucket | string;
}

export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

export interface SummaryView {
    totalCount: number;
    openCount: number;
    partialCount: number;
    paidCount: number;
    overdueCount: number;
    totalAmount: number;
    openAmount: number;
    partialAmount: number;
    paidAmount: number;
}
