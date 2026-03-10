import React from 'react';

export default function Tdashboard() {
	return (
		<div className="min-h-screen bg-slate-50">
			<div className="mx-auto max-w-6xl px-4 py-10">
				<div className="flex flex-col gap-2">
					<div className="text-xs font-semibold uppercase tracking-wider text-slate-500">Technician Dashboard</div>
					<h1 className="text-3xl font-extrabold text-slate-900">Operations Dashboard</h1>
					<div className="text-sm text-slate-500">Manage your bookings and requests</div>
				</div>

				<div className="mt-8 rounded-2xl border border-rose-200 bg-rose-50 p-6 shadow-sm">
					<div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
						<div className="flex gap-3">
							<div className="mt-0.5 flex h-8 w-8 items-center justify-center rounded-full bg-rose-100 text-rose-700">!</div>
							<div>
								<div className="text-sm font-extrabold text-rose-800">Account Verification Required</div>
								<div className="mt-1 text-xs leading-5 text-rose-700">
									Your account is currently unverified. You must complete the verification process to access bookings and accept requests. Please
									submit your required documents and wait for admin approval.
								</div>
							</div>
						</div>

						<div className="flex flex-col gap-2 sm:flex-row">
							<button className="rounded-lg bg-rose-600 px-4 py-2 text-xs font-bold text-white hover:bg-rose-700">
								Start Verification Process
							</button>
							<button className="rounded-lg border border-rose-300 bg-white px-4 py-2 text-xs font-bold text-rose-700 hover:bg-rose-100">
								Contact Support
							</button>
						</div>
					</div>
				</div>

				<div className="mt-8 rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">
					<div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full border border-slate-200 bg-slate-50 text-2xl">🕒</div>
					<div className="mt-4 text-sm font-semibold text-slate-700">No Active Bookings</div>
					<div className="mt-1 text-xs text-slate-500">
						Complete your verification to start accepting and managing bookings
					</div>
				</div>

				<div className="mt-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
					<div className="text-sm font-bold text-slate-900">Pending Requests</div>

					<div className="mt-4 space-y-3">
						{[
							{ name: 'Sarah Johnson', service: 'GPU Deep Clean', price: '₱150' },
							{ name: 'Michael Chen', service: 'Standard External Cleaning', price: '₱1,500' },
							{ name: 'Emily Rodriguez', service: 'Deep Internal Cleaning', price: '₱1,250' }
						].map((r) => (
							<div key={r.name} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
								<div className="flex items-center justify-between gap-4">
									<div>
										<div className="text-sm font-extrabold text-slate-900">{r.name}</div>
										<div className="mt-0.5 text-xs text-slate-500">{r.service}</div>
									</div>
									<div className="flex items-center gap-3">
										<div className="rounded-lg bg-slate-200 px-2.5 py-1 text-xs font-semibold text-slate-600">{r.price}</div>
										<button
											className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-500"
											disabled
										>
											Verification Required to Accept Requests
										</button>
									</div>
								</div>
							</div>
						))}
					</div>
				</div>
			</div>
		</div>
	);
}
