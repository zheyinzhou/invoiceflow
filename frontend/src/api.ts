const BASE = import.meta.env.VITE_API_BASE ?? '/api';
const ROOT = import.meta.env.VITE_API_ROOT ?? '/';

function qs(params: Record<string, any>) {
    const s = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== '') s.append(k, String(v));
    });
    return s.toString();
}

export async function fetchInvoices(params: {
    page?: number; size?: number; sort?: string;
    status?: string; overdue?: boolean; from?: string; to?: string; q?: string;
}) {
    const r = await fetch(`${BASE}/invoices?${qs(params)}`);
    if (!r.ok) {
        const body = await r.text().catch(() => '');
        throw new Error(`invoices ${r.status}: ${body || 'fetch failed'}`);
    }
    return r.json();
}

/** ✅ overdue aging */
export async function fetchAgingOverdue() {
    const r = await fetch(`${BASE}/invoices/aging/overdue`);
    if (!r.ok) {
        const body = await r.text().catch(() => '');
        throw new Error(`aging/overdue ${r.status}: ${body || 'fetch failed'}`);
    }
    return r.json();
}

/** overdue data */
export async function fetchOverdue(params: {
    page?: number; size?: number; q?: string;
    /** ASCII ："0-7"|"8-30"|"31-60"|"61-90"|">90" */
    bucket?: string;
}) {
    if (params.bucket) {
        params.bucket = params.bucket.replace(/[–—]/g, "-");
    }
    const r = await fetch(`${BASE}/invoices/overdue?${qs(params)}`);
    if (!r.ok) {
        const body = await r.text().catch(() => '');
        throw new Error(`overdue ${r.status}: ${body || 'fetch failed'}`);
    }
    return r.json();
}

export async function fetchSummary() {
    const r = await fetch(`${BASE}/summary`);
    if (!r.ok) {
        const body = await r.text().catch(() => '');
        throw new Error(`summary ${r.status}: ${body || 'fetch failed'}`);
    }
    return r.json();
}

export async function fetchOverdueByDue(params: {
    from: string;   // '2025-07-01'
    to: string;     // '2025-10-01'
    gran?: 'day' | 'week';
}) {
    const r = await fetch(`${BASE}/risk/kpi/overdue-by-due?${qs(params)}`);
    if (!r.ok) {
        const body = await r.text().catch(() => '');
        throw new Error(`kpi/overdue-by-due ${r.status}: ${body || 'fetch failed'}`);
    }
    return r.json() as Promise<Array<{ bucketDate: string; amount: number; count: number }>>;
}

export async function fetchRiskTop(params: {
    mode: 'amount'|'max_days'|'ratio',
    top?: number
}) {
    const s = new URLSearchParams(params as any).toString();
    const r = await fetch(`${BASE}/risk/customers?${s}`);
    if (!r.ok) throw new Error(await r.text());
    return r.json() as Promise<Array<{
        customer: string; invoices: number; total: number;
        overdue: number; maxDpd: number; ratio: number;
    }>>;
}

// ---- admin auth (Basic) ----
const ADMIN_KEY = 'admin_basic';
export function authHeader(): HeadersInit {
    const token = localStorage.getItem(ADMIN_KEY);
    return token ? { Authorization: token } : {};
}

// ---- admin ops ----
export async function adminCompany() {
    const r = await fetch(`${ROOT}/api/company`, {
        headers: authHeader(),
        credentials: 'include',
    });
    if (!r.ok) throw new Error('company fetch failed');
    return r.json();
}

export async function adminSync(batch = 200) {
    const r = await fetch(`${ROOT}/api/admin/sync-qbo?batch=${batch}`, {
        method: 'POST',
        headers: authHeader(),
        credentials: 'include',
    });
    const body = await r.json().catch(() => ({}));
    if (!r.ok) throw new Error(JSON.stringify(body));
    return body;
}
