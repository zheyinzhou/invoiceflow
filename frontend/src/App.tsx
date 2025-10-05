import { Routes, Route } from 'react-router-dom';
import Nav from './components/Nav';
import Dashboard from './pages/Dashboard';
import Admin from './pages/Admin';
import InvoiceInsights from "./pages/InvoiceInsights.tsx";

export default function App() {
    return (
        <>
            <Nav />
            <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/admin" element={<Admin />} />
                <Route path="/invoiceinsights" element={<InvoiceInsights />} />
            </Routes>
        </>
    );
}
