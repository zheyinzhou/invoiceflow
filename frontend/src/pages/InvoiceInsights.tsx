// InvoiceInsights.tsx
import {useEffect, useMemo, useState} from 'react';
import {
    Box,
    Card,
    CardContent,
    Grid,
    MenuItem,
    Select,
    ToggleButton,
    ToggleButtonGroup,
    Typography,
} from '@mui/material';
import {DataGrid, type GridColDef} from '@mui/x-data-grid';
import {
    Bar,
    BarChart,
    CartesianGrid,
    Legend,
    Line,
    LineChart,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from 'recharts';

import {
    fetchSummary,
    fetchAgingOverdue,
    fetchOverdueByDue,
    fetchRiskTop,
} from '../api';

import type {SummaryView} from '../types';
import {fmtMoney} from "../utils/format.ts";

type RiskModeUI = 'AMOUNT' | 'DAYS' | 'PCT';
type Gran = 'day' | 'week';

function formatDate(d: Date) {
    return d.toISOString().slice(0, 10);
}

export default function InvoiceInsights() {
    const [err, setErr] = useState<string | null>(null);

    const [, setSummaryState] = useState<SummaryView | null>(null);

    // overdue by week/day
    const [gran, setGran] = useState<Gran>('week');
    const today = new Date();
    const fromDefault = new Date(today);
    fromDefault.setDate(today.getDate() - 90);
    const [from] = useState<string>(formatDate(fromDefault));
    const [to] = useState<string>(formatDate(today));
    const [trend, setTrend] = useState<Array<{ date: string; amount: number; count: number }>>([]);

    // Overdue aging breakdown
    const [aging, setAging] = useState<{ bucket: string; amount: number; count: number }[]>([]);
    const [paginationModel, setPaginationModel] = useState({ page: 0, pageSize: 10 });

    // risk Top-N
    const [topN, setTopN] = useState<number>(10);
    const [mode, setMode] = useState<RiskModeUI>('AMOUNT'); // BY AMOUNT / BY MAX DAYS / BY %
    const [riskRows, setRiskRows] = useState<Array<{
        customer: string;
        invoices: number;
        total: number;
        overdue: number;
        maxDpd: number;
        ratio: number; // 0..1
    }>>([]);
    const [loadingRisk, setLoadingRisk] = useState(false);

    // init：summary + overdue aging
    useEffect(() => {
        (async () => {
            try {
                setErr(null);
                const s = await fetchSummary();
                setSummaryState(s);

                const ag = await fetchAgingOverdue();
                const agd = Object.entries(ag).map(([bucket, v]: any) => ({
                    bucket,
                    amount:Number(v.amount ?? 0),
                    count: Number(v.count ?? 0),
                }));
                setAging(agd);
            } catch (e: any) {
                setErr(`init failed: ${e?.message || e}`);
            }
        })();
    }, []);

    useEffect(() => {
        (async () => {
            try {
                const list = await fetchOverdueByDue({from, to, gran});
                setTrend(
                    list.map((x) => ({
                        date: x.bucketDate,
                        amount: Number(x.amount || 0),
                        count: Number(x.count || 0),
                    })),
                );
            } catch (e: any) {
                setErr(`trend fetch failed: ${e?.message || e}`);
            }
        })();
    }, [from, to, gran]);

    // risk
    useEffect(() => {
        (async () => {
            try {
                setLoadingRisk(true);
                const m = mode === 'AMOUNT' ? 'amount' : mode === 'DAYS' ? 'max_days' : 'ratio';
                const rows = await fetchRiskTop({mode: m, top: topN});
                setRiskRows(rows);
            } catch (e: any) {
                setErr(`risk top fetch failed: ${e?.message || e}`);
            } finally {
                setLoadingRisk(false);
            }
        })();
    }, [mode, topN]);

    // 右侧表格列
    const riskCols: GridColDef[] = useMemo(
        () => [
            {field: 'customer', headerName: 'Customer', flex: 1, minWidth: 180},
            {field: 'invoices', headerName: 'Invoices', width: 110, type: 'number'},
            {field: 'total', headerName: 'Total ($)', width: 130, type: 'number', valueFormatter: (p) => fmtMoney(p)},
            {field: 'overdue', headerName: 'Overdue ($)', width: 140, type: 'number', valueFormatter: (p) => fmtMoney(p)},
            {
                field: 'ratio',
                headerName: 'Overdue %',
                width: 120,
                type: 'number',
                valueFormatter: (p) => (Number(p) * 100).toFixed(1) + '%',
            },
            {field: 'maxDpd', headerName: 'Max DPD', width: 110, type: 'number'},
        ],
        [],
    );

    return (
        <Box sx={{px: 2, pb: 4, mt: 10}}>
            {err && (
                <Box sx={{mb: 2, p: 1.5, borderRadius: 1, bgcolor: '#fee', color: '#c00', border: '1px solid #f99'}}>
                    {err}
                </Box>
            )}

            <Grid container spacing={2}>
                {/* Overdue Amount Trend（Weekly / Daily） */}
                <Grid size={{xs: 12, md: 6}}>
                    <Card>
                        <CardContent>
                            <Box sx={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1}}>
                                <Typography variant="subtitle2" color="text.secondary">
                                    Overdue Amount Trend ({gran === 'week' ? 'weekly' : 'daily'}, grouped by due date)
                                </Typography>
                                <Box sx={{display: 'flex', gap: 1}}>
                                    <Select
                                        size="small"
                                        value={gran}
                                        onChange={(e) => setGran(e.target.value as Gran)}
                                        sx={{width: 120}}
                                    >
                                        <MenuItem value="week">Weekly</MenuItem>
                                        <MenuItem value="day">Daily</MenuItem>
                                    </Select>
                                </Box>
                            </Box>

                            <Box sx={{height: 300}}>
                                <ResponsiveContainer>
                                    <LineChart data={trend}>
                                        <CartesianGrid strokeDasharray="3 3"/>
                                        <XAxis dataKey="date"/>
                                        <YAxis/>
                                        <Tooltip formatter={(v: any, n: string) => (n === 'amount' ? fmtMoney(v) : v)}/>
                                        <Legend/>
                                        <Line type="monotone" dataKey="amount" name="Amount ($)" dot={false}/>
                                        {/* <Line yAxisId="right" type="monotone" dataKey="count" name="Count" dot={false} /> */}
                                    </LineChart>
                                </ResponsiveContainer>
                            </Box>
                            <Typography variant="caption" color="text.secondary">
                                Note: The backend groups **currently overdue** invoices by **due date** and aggregates
                                remaining
                                balances. Date range defaults to the last 90 days.
                            </Typography>
                        </CardContent>
                    </Card>

                    {/* 下：Aging Breakdown（Overdue only） */}
                    <Card sx={{mt: 2,ml:0}}>
                        <CardContent>
                            <Typography variant="subtitle2" color="text.secondary" sx={{mb: 1}}>
                                Aging Breakdown (Overdue only)
                            </Typography>
                            <Box sx={{height: 300}}>
                                <ResponsiveContainer>
                                    <BarChart data={aging} margin={{ left: 10, right: 12, top: 10, bottom: 24 }}>
                                        <CartesianGrid strokeDasharray="3 3"/>
                                        <XAxis dataKey="bucket"/>
                                        <YAxis/>
                                        <Tooltip formatter={(v: any, n: string) => (n === 'amount' ? fmtMoney(v) : v)}/>
                                        <Legend/>
                                        <Bar dataKey="amount" name="Amount ($)"/>
                                        <Bar dataKey="count" name="Count"/>
                                    </BarChart>
                                </ResponsiveContainer>
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>

                <Grid size={{xs: 12, md: 6}}>
                    <Card sx={{display: 'flex', flexDirection: 'column'}}>
                        <CardContent sx={{pb: 1}}>
                            <Box sx={{display: 'flex', alignItems: 'center', gap: 1, justifyContent: 'space-between'}}>
                                <Typography variant="subtitle2" color="text.secondary">
                                    Customer Risk Top-N
                                </Typography>
                                <Box sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                                    <ToggleButtonGroup
                                        size="small"
                                        value={mode}
                                        exclusive
                                        onChange={(_, v: RiskModeUI) => v && setMode(v)}
                                    >
                                        <ToggleButton value="AMOUNT">BY AMOUNT</ToggleButton>
                                        <ToggleButton value="DAYS">BY MAX DAYS</ToggleButton>
                                        <ToggleButton value="PCT">BY %</ToggleButton>
                                    </ToggleButtonGroup>
                                    <Box sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                                        <Typography variant="caption">Top N</Typography>
                                        <Select
                                            size="small"
                                            value={topN}
                                            onChange={(e) => setTopN(Number(e.target.value))}
                                            sx={{width: 80}}
                                        >
                                            {[5, 10, 20, 50].map((n) => (
                                                <MenuItem key={n} value={n}>
                                                    {n}
                                                </MenuItem>
                                            ))}
                                        </Select>
                                    </Box>
                                </Box>
                            </Box>
                        </CardContent>

                        <Box sx={{flex: 1, px: 2, pb: 2}}>
                            <DataGrid
                                rows={riskRows.map((r, i) => ({ id: i + 1, ...r }))}
                                columns={riskCols}
                                loading={loadingRisk}

                                pagination
                                paginationMode="client"
                                paginationModel={paginationModel}
                                onPaginationModelChange={setPaginationModel}
                                pageSizeOptions={[10, 25, 50]}

                                // autoHeight={false}
                                sx={{ height: '100%' }}
                                disableRowSelectionOnClick
                            />
                        </Box>

                        <Box
                            sx={{
                                px: 2,
                                pb: 1,
                                display: 'flex',
                                justifyContent: 'space-between',
                                color: 'text.secondary',
                            }}
                        >
                            <Typography variant="caption">
                                Showing
                                Top {topN} by {mode === 'AMOUNT' ? 'amount' : mode === 'DAYS' ? 'max days' : 'ratio'}
                            </Typography>
                            <Typography
                                variant="caption"
                                sx={{cursor: 'pointer'}}
                                onClick={() => {
                                    const m = mode === 'AMOUNT' ? 'amount' : mode === 'DAYS' ? 'max_days' : 'ratio';
                                    fetchRiskTop({mode: m, top: topN})
                                        .then(setRiskRows)
                                        .catch((e: any) => setErr(`risk top fetch failed: ${e?.message || e}`));
                                }}
                            >
                                Refresh
                            </Typography>
                        </Box>
                    </Card>
                </Grid>
            </Grid>
        </Box>
    );
}
