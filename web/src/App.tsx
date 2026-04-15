import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/login/Login';
import Register from './pages/register/Register';
import RoleSelection from './pages/auth/RoleSelection';
import AuthCallback from './pages/auth/AuthCallback';
import Dashboard from './pages/dashboard/Dashboard';
import Tdashboard from './pages/dashboard/Tdashboard';
import Adashboard from './pages/dashboard/Adashboard';
import Booking from './pages/booking/Booking';

function getRole(): string | null {
	try {
		const raw = localStorage.getItem('cleanit.user');
		return raw ? JSON.parse(raw)?.role ?? null : null;
	} catch {
		return null;
	}
}

function hasToken(): boolean {
	return !!localStorage.getItem('cleanit.token');
}

function ProtectedRoute({ element, allowedRole }: { element: JSX.Element; allowedRole: string }) {
	const role = getRole();
	const token = hasToken();
	if (!token || !role) return <Navigate to="/login" replace />;
	if (role !== allowedRole) {
		if (role === 'admin') return <Navigate to="/admin/dashboard" replace />;
		if (role === 'technician') return <Navigate to="/dashboard/technician" replace />;
		return <Navigate to="/dashboard" replace />;
	}
	return element;
}

function GuestRoute({ element }: { element: JSX.Element }) {
	const role = getRole();
	const token = hasToken();
	if (token && role) {
		if (role === 'admin') return <Navigate to="/admin/dashboard" replace />;
		if (role === 'technician') return <Navigate to="/dashboard/technician" replace />;
		return <Navigate to="/dashboard" replace />;
	}
	return element;
}

export default function App() {
	return (
		<div className="min-h-screen bg-slate-50">
			<Router>
				<Routes>
					<Route path="/" element={<Navigate to="/login" replace />} />
					<Route path="/login" element={<GuestRoute element={<Login />} />} />
					<Route path="/register" element={<GuestRoute element={<Register />} />} />
					<Route path="/role-selection" element={<RoleSelection />} />
					<Route path="/auth/callback" element={<AuthCallback />} />
					<Route path="/dashboard" element={<ProtectedRoute element={<Dashboard />} allowedRole="client" />} />
					<Route path="/dashboard/technician" element={<ProtectedRoute element={<Tdashboard />} allowedRole="technician" />} />
					<Route path="/admin/dashboard" element={<ProtectedRoute element={<Adashboard />} allowedRole="admin" />} />
					<Route path="/booking/:serviceId" element={<ProtectedRoute element={<Booking />} allowedRole="client" />} />
					<Route path="*" element={<Navigate to="/login" replace />} />
				</Routes>
			</Router>
		</div>
	);
}
