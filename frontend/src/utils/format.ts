const locale = (typeof navigator !== 'undefined' && navigator.language) || 'en-US';
const numberFmt = new Intl.NumberFormat(locale, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
});

export function fmtMoney(v: unknown): string {
    if (v == null || v === '') return '--';
    const num =
        typeof v === 'number'
            ? v
            : Number(String(v).replace(/,/g, '').trim());
    if (!Number.isFinite(num)) return '--';
    return numberFmt.format(num);
}