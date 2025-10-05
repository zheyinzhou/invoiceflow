import {useState} from 'react';
import {Box, Card, CardContent, Typography, Button, Alert, Divider, Paper, Grid} from '@mui/material';
import {adminCompany, adminSync} from '../api';

const API_ROOT = 'http://localhost:8080';

export default function Admin() {
    const [msg, setMsg] = useState<string | null>(null);
    const [result, setResult] = useState<any>(null);
    const [loadingCompany, setLoadingCompany] = useState(false);
    const [syncing, setSyncing] = useState(false);

    return (
        <>
            <Box sx={{px: 2, pb: 4, mt:2}}>
                <Grid container spacing={2} sx={{mb: 2}}>
                    <Grid size={{xs: 12, md: 6}}>
                        <Card>
                            <CardContent>
                                <Typography variant="subtitle2" color="text.secondary">Connect to
                                    QuickBooks</Typography>
                                <Typography variant="body2" color="text.secondary" sx={{mb: 1}}>
                                </Typography>
                                <Button variant="contained"
                                        onClick={() => (window.location.href = `${API_ROOT}/connect`)}>
                                    Connect
                                </Button>
                            </CardContent>
                        </Card>
                    </Grid>

                    <Grid size={{xs: 12, md: 6}}>
                        <Card>
                            <CardContent>
                                <Typography variant="subtitle2" color="text.secondary">Try to fetch Company
                                    API</Typography>
                                <Typography variant="body2" color="text.secondary" sx={{mb: 1}}>
                                </Typography>
                                <Button variant="outlined" disabled={loadingCompany} onClick={async () => {
                                    setLoadingCompany(true);
                                    setMsg(null);
                                    try {
                                        const data = await adminCompany();
                                        setResult(data);
                                        setMsg('Company fetched.');
                                    } catch (e: any) {
                                        setMsg(e?.message || 'Company failed');
                                    } finally {
                                        setLoadingCompany(false);
                                    }
                                }}>
                                    {loadingCompany ? 'Loading…' : 'Fetch'}
                                </Button>
                            </CardContent>
                        </Card>
                    </Grid>

                    <Grid size={{xs: 12, md: 6}}>
                        <Card>
                            <CardContent>
                                <Typography variant="subtitle2" color="text.secondary">Sync from QBO</Typography>
                                <Typography variant="body2" color="text.secondary" sx={{mb: 1}}>
                                </Typography>
                                <Button variant="contained" disabled={syncing} onClick={async () => {
                                    setSyncing(true);
                                    setMsg(null);
                                    try {
                                        const r = await adminSync(200);
                                        setResult(r);
                                        setMsg(`Synced: upserts=${r?.upserts ?? '?'}`);
                                    } catch (e: any) {
                                        setMsg(e?.message || 'Sync failed');
                                    } finally {
                                        setSyncing(false);
                                    }
                                }}>
                                    {syncing ? 'Syncing…' : 'Sync'}
                                </Button>
                            </CardContent>
                        </Card>
                    </Grid>
                </Grid>

                {msg && <Alert severity="info" sx={{mb: 2}}>{msg}</Alert>}

                <Card>
                    <CardContent>
                        <Typography variant="subtitle2" color="text.secondary">Result</Typography>
                        <Divider sx={{my: 1}}/>
                        <Paper variant="outlined"
                               sx={{p: 1.5, bgcolor: '#0f1220', color: '#e7e7ea', borderRadius: 1, overflow: 'auto'}}>
                            <pre style={{margin: 0}}>{JSON.stringify(result, null, 2)}</pre>
                        </Paper>
                    </CardContent>
                </Card>
            </Box>
        </>
    );
}
