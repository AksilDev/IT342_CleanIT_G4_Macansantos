import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

export default function Dashboard() {
	const navigate = useNavigate();
	const [searchParams] = useSearchParams();
	
	// Handle OAuth redirect data
	useEffect(() => {
		const oauth = searchParams.get('oauth');
		const email = searchParams.get('user');
		const role = searchParams.get('role');
		
		if (oauth === 'success' && email && role) {
			// Store OAuth user data in localStorage
			const userData = {
				email: decodeURIComponent(email),
				role: role,
				name: decodeURIComponent(email).split('@')[0] // temporary name from email
			};
			localStorage.setItem('cleanit.user', JSON.stringify(userData));
			// Clean up URL
			navigate('/dashboard', { replace: true });
			window.location.reload(); // reload to refresh user data
		}
	}, [searchParams, navigate]);
	
	const user = (() => {
		try {
			const raw = localStorage.getItem('cleanit.user');
			return raw ? JSON.parse(raw) : null;
		} catch {
			return null;
		}
	})();

	// Redirect to login if no user data
	React.useEffect(() => {
		if (!user) {
			navigate('/login');
		}
	}, [user, navigate]);

	if (!user) return null;

	const handleLogout = () => {
		localStorage.removeItem('cleanit.user');
		navigate('/login');
	};

	return (
		<div className="min-h-screen bg-slate-50">
			<div className="mx-auto max-w-6xl px-4 py-10">
				<div className="flex flex-col gap-6 md:flex-row md:items-start md:justify-between">
					<div>
						<h1 className="text-3xl font-extrabold text-slate-900">Welcome Back, {user?.name || 'User'}!</h1>
						<div className="mt-1 text-sm text-slate-500">Manage your bookings and explore our services</div>
					</div>

					<div className="w-full max-w-sm rounded-xl border border-violet-200 bg-white p-4 shadow-sm">
						<div className="flex items-center gap-3">
							<div className="flex h-11 w-11 items-center justify-center rounded-full bg-violet-700 text-sm font-bold text-white">
								{String(user?.name || 'User')
									.split(' ')
									.map((s: string) => s[0])
									.join('')
									.toUpperCase()
									.slice(0, 2)}
							</div>
							<div className="flex-1">
								<div className="text-sm font-semibold text-slate-800">{user?.name || 'Guest User'}</div>
								<div className="text-xs text-slate-500">{user?.email || 'No email'}</div>
								<div className="text-xs text-slate-500">{user?.contactNo || 'No contact info'}</div>
							</div>
							<div className="rounded-full bg-rose-50 px-2.5 py-1 text-xs font-semibold text-rose-700">Unverified</div>
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

				<div className="mt-10 space-y-8">
					<section>
						<div className="text-lg font-bold text-slate-900">Active Booking</div>
						<div className="mt-3 rounded-2xl border border-slate-200 bg-white p-10 text-center">
							<div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-slate-100 text-2xl">🕒</div>
							<div className="mt-3 text-sm font-semibold text-slate-700">No Active Bookings</div>
							<div className="mt-1 text-xs text-slate-500">You currently have no active bookings. Verify your account to start booking our services.</div>
						</div>
					</section>

					<section>
						<div className="flex items-center justify-between gap-4">
							<div className="text-lg font-bold text-slate-900">Browse Services</div>
							<button className="rounded-lg border border-violet-300 bg-white px-3 py-1.5 text-xs font-semibold text-violet-700 hover:bg-violet-50">
								View All Services →
							</button>
						</div>

						<div className="mt-4 grid grid-cols-1 gap-5 md:grid-cols-3">
							{[
								{ name: 'Standard External Cleaning', price: '₱200', time: '1-2 hours' },
								{ name: 'Deep Internal Cleaning', price: '₱1,250', time: '2-3 hours' },
								{ name: 'GPU Deep Cleaning', price: '₱600', time: '1.5-2 hours' }
							].map((s) => (
								<div key={s.name} className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
									<div className="flex h-40 items-center justify-center bg-violet-100 text-slate-400">Service Image</div>
									<div className="p-4">
										<div className="text-sm font-bold text-slate-900">{s.name}</div>
										<div className="mt-1 text-xs text-slate-500">Complete external cleaning service</div>
										<div className="mt-3 flex items-center justify-between">
											<div className="text-xs text-slate-500">🕒 {s.time}</div>
											<div className="text-sm font-extrabold text-violet-700">{s.price}</div>
										</div>
									</div>
								</div>
							))}
						</div>
					</section>

					<section>
						<div className="text-lg font-bold text-slate-900">Booking History</div>
						<div className="mt-3 rounded-2xl border border-slate-200 bg-white p-10 text-center">
							<div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-slate-100 text-2xl">📅</div>
							<div className="mt-3 text-sm font-semibold text-slate-700">No Booking History</div>
							<div className="mt-1 text-xs text-slate-500">You haven&apos;t made any bookings yet. Verify your account to get started.</div>
						</div>
					</section>
				</div>
			</div>
		</div>
	);
}
