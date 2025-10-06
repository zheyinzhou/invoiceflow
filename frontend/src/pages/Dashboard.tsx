import {useEffect, useMemo, useState} from 'react';
import {fetchInvoices, fetchSummary, fetchAgingOverdue, fetchOverdue} from '../api';
import type {InvoiceView, Page, SummaryView} from '../types';
import {Grid, Card, CardContent, Typography, Box} from '@mui/material';
import {DataGrid, type GridColDef} from '@mui/x-data-grid';
import {
    PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer,
    BarChart, Bar, XAxis, YAxis, CartesianGrid
} from 'recharts';
import {fmtMoney} from "../utils/format.ts";

export default function Dashboard() {
    const [summary, setSummary] = useState<SummaryView | null>(null);
    const [page, setPage] = useState(0);
    const [status, setStatus] = useState('');
    const [table, setTable] = useState<Page<InvoiceView> | null>(null);
    const [q] = useState('');


    const [overdueOnly, setOverdueOnly] = useState(false);
    const [bucketFilter, setBucketFilter] = useState<string | null>(null);

    // aging /invoices/aging/overdue
    const [aging, setAging] = useState<{ bucket: string; amount: number; count: number }[]>([]);

    useEffect(() => {
        fetchSummary().then(setSummary).catch(console.error);
    }, []);

    useEffect(() => {
        const run = async () => {
            try {
                if (overdueOnly || bucketFilter) {
                    const data = await fetchOverdue({
                        page, size: 10, q,
                        bucket: bucketFilter || undefined, //  ASCII "-"
                    });
                    setTable(data);
                } else {
                    const data = await fetchInvoices({page, size: 10, status, q});
                    setTable(data);
                }
            } catch (e) {
                console.error(e);
            }
        };
        run();
    }, [page, status, q, overdueOnly, bucketFilter]);

    useEffect(() => {
        fetchAgingOverdue()
            .then((res) => {
                const data = Object.entries(res).map(([bucket, v]: any) => ({
                    bucket,
                    amount: Number(v.amount ?? 0),
                    count: Number(v.count ?? 0),
                }));
                setAging(data);
            })
            .catch(console.error);
    }, []);

    const pieData = useMemo(() => {
        if (!summary) return [];
        return [
            {name: 'OPEN', value: summary.openCount},
            {name: 'PARTIAL_PAID', value: summary.partialCount},
            {name: 'PAID', value: summary.paidCount},
        ];
    }, [summary]);

    const STATUS_ORDER = ['OPEN', 'PAID', 'PARTIAL_PAID', 'OVERDUE'] as const;
    const STATUS_COLORS: Record<string, string> = {
        OPEN: '#3B82F6',        // blue
        PAID: '#F59E0B',        // orange
        PARTIAL_PAID: '#10B981',// green
    };

    const pieSeries = useMemo(() => {
        const m = new Map(pieData.map(d => [d.name, d.value]));
        return STATUS_ORDER
            .map(name => ({ name, value: m.get(name) ?? 0 }))
            .filter(d => d.value > 0);
    }, [pieData]);

    // DataGrid
    const baseCols: GridColDef[] = [
        {field: 'id', headerName: 'ID', width: 90},
        {field: 'customerName', headerName: 'Customer', flex: 1, minWidth: 180},
        {field: 'status', headerName: 'Status', width: 140},
        {field: 'totalAmt', headerName: 'Total', width: 120, type: 'number', valueFormatter: (p) => fmtMoney(p)},
        {field: 'balance', headerName: 'Balance', width: 120, type: 'number', valueFormatter: (p) => fmtMoney(p)},
        {field: 'dueDate', headerName: 'Due', width: 140},
        {field: 'overdue', headerName: 'Overdue', width: 110, valueFormatter: (p) => (p ? 'YES' : 'NO')},
    ];
    const extraCols: GridColDef[] = [];
    if (table?.content?.some(r => typeof (r as any).daysUntilDue === 'number')) {
        extraCols.push({field: 'daysUntilDue', headerName: 'DaysUntilDue', width: 140, type: 'number'});
    }
    if (table?.content?.some(r => (r as any).bucket)) {
        extraCols.push({field: 'bucket', headerName: 'Bucket', width: 120});
    }
    const columns: GridColDef[] = [...baseCols, ...extraCols];

    const rows: InvoiceView[] = useMemo(() => {
        let list = (table?.content ?? []).map((r: any) => ({
            ...r,
            bucket: r.bucket ?? r.agingBucket,
        }));

        if (overdueOnly) list = list.filter((r: any) => r.overdue);
        if (bucketFilter) list = list.filter((r: any) => r.bucket === bucketFilter);
        return list as InvoiceView[];
    }, [table, overdueOnly, bucketFilter]);

    const handleCardClick = (key: 'OPEN' | 'PARTIAL_PAID' | 'PAID' | 'OVERDUE') => {
        setPage(0);
        setBucketFilter(null);
        if (key === 'OVERDUE') {
            setStatus('');
            setOverdueOnly(true);
        } else {
            setStatus(key);
            setOverdueOnly(false);
        }
    };

    return (
        <Box sx={{px: 2, pb: 4, mt: 10}}>
            <Grid container spacing={2} sx={{mb: 1}}>
                {summary && [
                    {label: 'OPEN', v: summary.openCount},
                    {label: 'PARTIAL_PAID', v: summary.partialCount},
                    {label: 'PAID', v: summary.paidCount},
                    {label: 'OVERDUE', v: summary.overdueCount},
                ].map((c, i) => (
                    <Grid key={i} size={{xs: 12, md: 3, sm: 6}}>
                        <Card
                            sx={{height: '100%', cursor: 'pointer', transition: '0.2s', '&:hover': {boxShadow: 6}}}
                            onClick={() => handleCardClick(c.label as any)}
                        >
                            <CardContent>
                                <Typography variant="caption" color="text.secondary">{c.label}</Typography>
                                <Typography variant="h4" sx={{mt: .5, fontWeight: 700}}>{c.v}</Typography>
                            </CardContent>
                        </Card>
                    </Grid>
                ))}
            </Grid>

            <Box
                sx={{
                    display: 'grid',
                    gridTemplateColumns: '40% 60%',
                    gap: 2,
                    alignItems: 'start',
                }}
            >

                <Box>
                    <Card>
                        <CardContent sx={{height: 300}}>
                            <Typography variant="subtitle2" color="text.secondary" sx={{mb: 1}}>
                                Invoices by Status
                            </Typography>
                            <Box sx={{height: 260}}>
                                <ResponsiveContainer>
                                    <PieChart>
                                        <Pie
                                            data={pieSeries}
                                            dataKey="value"
                                            nameKey="name"
                                            onClick={(d:any) => {
                                                const name = (d?.name as string) || '';
                                                setStatus(name);
                                                setOverdueOnly(false);
                                                setBucketFilter(null);
                                                setPage(0);
                                            }}
                                        >
                                            {pieSeries.map((s) => (
                                                <Cell key={s.name} fill={STATUS_COLORS[s.name]} />
                                            ))}
                                        </Pie>
                                            <Tooltip/>
                                            <Legend/>
                                    </PieChart>
                                </ResponsiveContainer>
                            </Box>
                        </CardContent>
                    </Card>

                    <Card sx={{mt: 2}}>
                        <CardContent sx={{height: 300}}>
                            <Typography variant="subtitle2" color="text.secondary" sx={{mb: 1}}>
                                Overdue Aging
                            </Typography>
                            <Box sx={{height: 240}}>
                                <ResponsiveContainer>
                                    <BarChart
                                        data={aging}
                                        margin={{top: 10, right: 10, left: 20, bottom: 10}}
                                        onClick={(e: any) => {
                                            let b = e?.activeLabel as string | undefined;
                                            if (b) {
                                                b = b.replace(/[–—]/g, '-');
                                                setBucketFilter(b);
                                                setOverdueOnly(true);
                                                setPage(0);
                                            }
                                        }}
                                    >
                                        <CartesianGrid strokeDasharray="3 3"/>
                                        <XAxis dataKey="bucket"/>
                                        <YAxis/>
                                        <Tooltip
                                            formatter={(val: any, name: string) => name === 'amount' ? fmtMoney(val) : val}/>
                                        <Bar dataKey="amount" name="Amount ($)"/>
                                        <Bar dataKey="count" name="Count"/>
                                    </BarChart>
                                </ResponsiveContainer>
                            </Box>
                        </CardContent>
                    </Card>
                </Box>

                <Card
                    sx={{
                        height: 'calc(100vh - 230px)',
                        overflow: 'hidden',
                    }}
                >
                    <CardContent sx={{height: '100%', pt: 1}}>
                        <Typography variant="subtitle2" color="text.secondary" sx={{mb: 1}}>
                            {overdueOnly ? `Invoices (Overdue${bucketFilter ? ` · ${bucketFilter}` : ''})` : 'Invoices'}
                        </Typography>
                        <Box sx={{width: '100%', height: 'calc(100% - 26px)'}}>
                            <DataGrid
                                rows={rows}
                                columns={columns}
                                paginationMode="server"
                                rowCount={table?.totalElements || 0}
                                pageSizeOptions={[10]}
                                paginationModel={{page, pageSize: 10}}
                                onPaginationModelChange={(m) => setPage(m.page)}
                                disableRowSelectionOnClick
                                sx={{height: '100%'}}
                            />
                        </Box>
                    </CardContent>
                </Card>
            </Box>
        </Box>
);
}
