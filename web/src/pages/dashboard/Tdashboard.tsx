import React from 'react';
import { useNavigate } from 'react-router-dom';

export default function Tdashboard() {
	const navigate = useNavigate();
	
	const user = (() => {
		try {
			const raw = localStorage.getItem('cleanit.user');
			return raw ? JSON.parse(raw) : null;
		} catch {
			return null;
		}
	})();

	const handleLogout = () => {
		localStorage.removeItem('cleanit.user');
		navigate('/login');
	};

	return (
		<div className="min-h-screen bg-slate-50">
			<div className="mx-auto max-w-6xl px-4 py-10">
				<div className="flex flex-col gap-6 md:flex-row md:items-start md:justify-between">
					<div className="flex flex-col gap-2">
						<div className="text-xs font-semibold uppercase tracking-wider text-slate-500">Technician Dashboard</div>
						<h1 className="text-3xl font-extrabold text-slate-900">Welcome Back, {user?.name || 'Technician'}!</h1>
						<div className="text-sm text-slate-500">Manage your bookings and requests</div>
					</div>

					<div className="w-full max-w-sm rounded-xl border border-violet-200 bg-white p-4 shadow-sm">
						<div className="flex items-center gap-3">
							<div className="flex h-11 w-11 items-center justify-center rounded-full bg-violet-700 text-sm font-bold text-white">
								{String(user?.name || 'Tech')
									.split(' ')
									.map((s: string) => s[0])
									.join('')
									.toUpperCase()
									.slice(0, 2)}
							</div>
							<div className="flex-1">
								<div className="text-sm font-semibold text-slate-800">{user?.name || 'Technician'}</div>
								<div className="text-xs text-slate-500">{user?.email || 'No email'}</div>
								<div className="text-xs text-slate-500">{user?.contactNo || 'No contact info'}</div>
							</div>
							<div className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
								user?.verified 
									? 'bg-emerald-100 text-emerald-700' 
									: 'bg-rose-100 text-rose-700'
							}`}>
								{user?.verified ? 'Verified' : 'Unverified'}
							</div>
						</div>
						<div className="mt-4 flex gap-2">
							<button
								onClick={handleLogout}
								className="flex-1 rounded-lg border border-rose-300 bg-rose-50 px-3 py-2 text-xs font-semibold text-rose-700 hover:bg-rose-100 transition-colors"
							>
								Logout
							</button>
						</div>
					</div>
				</div>

				{!user?.verified && (
					<div className="mt-8 rounded-2xl border border-rose-200 bg-rose-50 p-6 shadow-sm">
						<div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
							<div className="flex gap-3">
								<div className="mt-0.5 flex h-8 w-8 items-center justify-center rounded-full bg-rose-100 text-rose-700">!</div>
								<div>
									<div className="text-sm font-extrabold text-rose-800">Account Verification Required</div>
									<div className="mt-1 text-xs leading-5 text-rose-700">
										Your account is currently unverified. You must complete the verification process to access bookings and accept requests. Please
										wait for <b>ADMIN APPROVAL</b>.
									</div>
								</div>
							</div>

							<div className="flex flex-col gap-2 sm:flex-row">
							</div>
						</div>
					</div>
				)}

				<div className="mt-8 rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">
					<div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full border border-slate-200 bg-slate-50 text-2xl">🕒</div>
					<div className="mt-4 text-sm font-semibold text-slate-700">No Active Bookings</div>
					<div className="mt-1 text-xs text-slate-500">
						Complete your verification to start accepting and managing bookings
					</div>
				</div>

				<div className="mt-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
					<div className="text-sm font-bold text-slate-900">Pending Requests</div>

					<div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-10 text-center">
						<div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-slate-100 text-2xl">📋</div>
						<div className="mt-3 text-sm font-semibold text-slate-700">No Pending Requests</div>
						<div className="mt-1 text-xs text-slate-500">
							There are no service requests pending at the moment
						</div>
					</div>
				</div>
			</div>
		</div>
	);
}
