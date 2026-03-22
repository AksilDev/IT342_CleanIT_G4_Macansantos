import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/login/Login';
import Register from './pages/register/Register';
import RoleSelection from './pages/auth/RoleSelection';
import Dashboard from './pages/dashboard/Dashboard';
import Tdashboard from './pages/dashboard/Tdashboard';
import Adashboard from './pages/dashboard/Adashboard';

export default function App() {
	return (
		<div className="min-h-screen bg-slate-50">
			<Router>
				<Routes>
					<Route path="/" element={<Navigate to="/login" replace />} />
					<Route path="/login" element={<Login />} />
					<Route path="/register" element={<Register />} />
					<Route path="/role-selection" element={<RoleSelection />} />
					<Route path="/dashboard" element={<Dashboard />} />
					<Route path="/dashboard/technician" element={<Tdashboard />} />
					<Route path="/admin/dashboard" element={<Adashboard />} />
					<Route path="*" element={<Navigate to="/login" replace />} />
				</Routes>
			</Router>
		</div>
	);
}
