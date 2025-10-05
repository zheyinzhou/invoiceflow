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

    // 后端若已提供，就接住；若没有也不影响渲染
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

/** ✅ 与后端 /invoices/aging 与 /invoices/aging/overdue 的返回保持一致 */
export type AgingResponse = Record<AgingBucket, { count: number; amount: number }>;
