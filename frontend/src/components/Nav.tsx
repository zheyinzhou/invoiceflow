import { AppBar, Toolbar, Typography, Box,Link } from '@mui/material';
import { Link as RouterLink} from 'react-router-dom';

export default function Nav() {

    return (
        <>
            <AppBar position="fixed" elevation={0} sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
                <Toolbar>
                    <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 700 }}>InvoiceFlow</Typography>
                    <Box sx={{ display: 'flex', gap: 2 }}>
                        <Link component={RouterLink} to="/admin" color="inherit" underline="none">Admin</Link>
                        <Link component={RouterLink} to="/" color="inherit" underline="none">Dashboard</Link>
                        <Link component={RouterLink} to="/invoiceinsights" color="inherit" underline="none">Invoice Insights</Link>
                    </Box>
                </Toolbar>
            </AppBar>

        </>
    );
}
