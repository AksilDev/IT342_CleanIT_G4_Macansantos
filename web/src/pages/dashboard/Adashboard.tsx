import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import BookingDetailsModal from '../../components/BookingDetailsModal';

interface User {
	id: string;
	name: string;
	email: string;
	contactNo: string;
	role: string;
	imageUrl: string | null;
	createdAt: string;
	verified?: boolean;
}

interface Statistics {
	activeBookings: number;
	confirmedBookings: number;
	pending: number;
	in_progress: number;
	completed: number;
	cancelled: number;
	total: number;
	today: number;
	thisWeek: number;
	thisMonth: number;
	totalRevenue: number;
	monthRevenue: number;
}

export default function Adashboard() {
	const navigate = useNavigate();
	const [pendingUsers, setPendingUsers] = useState<User[]>([]);
	const [selectedUser, setSelectedUser] = useState<User | null>(null);
	const [loading, setLoading] = useState(false);
	const [fetchLoading, setFetchLoading] = useState(true);
	const [statistics, setStatistics] = useState<Statistics>({
		activeBookings: 0,
		confirmedBookings: 0,
		pending: 0,
		in_progress: 0,
		completed: 0,
		cancelled: 0,
		total: 0,
		today: 0,
		thisWeek: 0,
		thisMonth: 0,
		totalRevenue: 0,
		monthRevenue: 0
	});
	const [statsLoading, setStatsLoading] = useState(true);
	const [statsError, setStatsError] = useState<string | null>(null);

	// Modal state for booking drill-down
	const [modalOpen, setModalOpen] = useState(false);
	const [modalTitle, setModalTitle] = useState('');
	const [modalStatuses, setModalStatuses] = useState<string[]>([]);
	const [modalInitialCount, setModalInitialCount] = useState(0);

	const user = React.useMemo(() => {
		try {
			const raw = localStorage.getItem('cleanit.user');
			return raw ? JSON.parse(raw) : null;
		} catch {
			return null;
		}
	}, []);

	// Redirect to login if no user data
	useEffect(() => {
		if (!user || user.role !== 'admin') {
			navigate('/login');
		}
	}, [user, navigate]);

	// Fetch pending users from backend
	useEffect(() => {
		const fetchPendingUsers = async () => {
			try {
				setFetchLoading(true);
				const response = await api.get('/v1/admin/pending-verifications');
				console.log('Pending users response:', response.data);
				// Log imageUrl for each user for debugging
				response.data.forEach((user: User) => {
					console.log(`User ${user.name} imageUrl:`, user.imageUrl);
				});
				setPendingUsers(response.data);
			} catch (err) {
				console.error('Failed to fetch pending users:', err);
			} finally {
				setFetchLoading(false);
			}
		};
		
		if (user && user.role === 'admin') {
			fetchPendingUsers();
		}
	}, [user]);

	// Fetch booking statistics from backend
	useEffect(() => {
		const fetchStatistics = async () => {
			try {
				setStatsLoading(true);
				setStatsError(null);
				const response = await api.get('/v1/admin/dashboard/statistics');
				setStatistics(response.data);
			} catch (err) {
				console.error('Failed to fetch statistics:', err);
				setStatsError('Failed to load statistics');
			} finally {
				setStatsLoading(false);
			}
		};
		
		if (user && user.role === 'admin') {
			fetchStatistics();
		}
	}, [user]);

	if (!user) return null;

	const handleLogout = () => {
		localStorage.removeItem('cleanit.user');
		localStorage.removeItem('cleanit.token');
		navigate('/login');
	};

	const handleApprove = async (userId: string) => {
		try {
			setLoading(true);
			await api.post(`/v1/admin/verify-user/${userId}`, { status: 'approved' });
			setPendingUsers(prev => prev.filter(u => u.id !== userId));
			setSelectedUser(null);
		} catch (err) {
			console.error('Failed to approve user:', err);
			alert('Failed to approve user');
		} finally {
			setLoading(false);
		}
	};

	const handleReject = async (userId: string) => {
		try {
			setLoading(true);
			await api.post(`/v1/admin/verify-user/${userId}`, { status: 'rejected' });
			setPendingUsers(prev => prev.filter(u => u.id !== userId));
			setSelectedUser(null);
		} catch (err) {
			console.error('Failed to reject user:', err);
			alert('Failed to reject user');
		} finally {
			setLoading(false);
		}
	};

	const handleCardClick = (title: string, statuses: string[], count: number) => {
		setModalTitle(title);
		setModalStatuses(statuses);
		setModalInitialCount(count);
		setModalOpen(true);
	};

	const handleModalClose = () => {
		setModalOpen(false);
	};

	const stats = [
		{
			title: 'Pending User Verifications',
			value: pendingUsers.filter(u => u.role === 'client').length,
			icon: '👤',
			dotColor: 'bg-red-500',
			clickable: false
		},
		{
			title: 'Pending Technician Approvals',
			value: pendingUsers.filter(u => u.role === 'technician').length,
			icon: '👥',
			dotColor: 'bg-orange-500',
			clickable: false
		},
		{
			title: 'Active Bookings',
			value: statsLoading ? '...' : statistics.activeBookings,
			icon: '🕐',
			dotColor: 'bg-blue-500',
			clickable: true,
			statuses: ['pending', 'confirmed', 'in_progress']
		},
		{
			title: 'Confirmed Bookings',
			value: statsLoading ? '...' : statistics.confirmedBookings,
			icon: '📅',
			dotColor: 'bg-green-500',
			clickable: true,
			statuses: ['confirmed']
		}
	];

	return (
		<div className="min-h-screen bg-white">
			{/* Header */}
			<header className="border-b border-gray-200 bg-white px-6 py-4 shadow-sm">
				<div className="mx-auto flex max-w-6xl items-center justify-between">
					<div className="text-xl font-bold text-violet-700">CLEAN-IT</div>
					<div className="flex items-center gap-3">
						<div className="flex items-center gap-3 rounded-lg border border-violet-600 bg-violet-50 px-4 py-2">
							<div className="flex h-8 w-8 items-center justify-center rounded-full bg-violet-700 text-xs font-bold text-white">
								{String(user?.name || 'Admin')
									.split(' ')
									.map((s: string) => s[0])
									.join('')
									.toUpperCase()
									.slice(0, 2)}
							</div>
							<span className="text-sm font-medium text-violet-900">{user?.name || 'Admin'}</span>
						</div>
						<button
							onClick={handleLogout}
							className="rounded-lg border border-rose-300 bg-rose-50 px-3 py-2 text-xs font-semibold text-rose-700 hover:bg-rose-100 transition-colors"
						>
							Logout
						</button>
					</div>
				</div>
			</header>

			{/* Main Content */}
			<main className="mx-auto max-w-6xl px-6 py-8">
				{/* Title Section */}
				<div className="mb-8">
					<h1 className="text-3xl font-bold text-slate-900">Admin Dashboard</h1>
					<p className="mt-1 text-sm text-slate-500">Manage verifications, approvals, and monitor bookings</p>
				</div>

				{/* Stats Cards */}
				<div className="mb-8 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
					{statsError && (
						<div className="col-span-full rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">
							{statsError}
						</div>
					)}
					{stats.map((stat) => (
						<div
							key={stat.title}
							className={`rounded-xl border border-gray-100 bg-white p-5 shadow-md ${
								stat.clickable ? 'cursor-pointer hover:shadow-lg transition-shadow' : ''
							}`}
							onClick={() => {
								if (stat.clickable && stat.statuses && typeof stat.value === 'number') {
									handleCardClick(stat.title, stat.statuses, stat.value);
								}
							}}
							onKeyDown={(e) => {
								if (stat.clickable && (e.key === 'Enter' || e.key === ' ')) {
									e.preventDefault();
									if (stat.statuses && typeof stat.value === 'number') {
										handleCardClick(stat.title, stat.statuses, stat.value);
									}
								}
							}}
							tabIndex={stat.clickable ? 0 : undefined}
							role={stat.clickable ? 'button' : undefined}
							aria-label={stat.clickable ? `View ${stat.title} details` : undefined}
						>
							<div className="flex items-start justify-between">
								<div className="flex h-10 w-10 items-center justify-center rounded-lg bg-violet-100 text-lg">
									{stat.icon}
								</div>
								<div className={`h-2.5 w-2.5 rounded-full ${stat.dotColor}`} />
							</div>
							<div className="mt-4">
								<div className="text-2xl font-bold text-slate-900">{stat.value}</div>
								<div className="mt-1 text-xs text-slate-500">{stat.title}</div>
							</div>
						</div>
					))}
				</div>

				{/* User Verification Section */}
				<div className="mb-8">
					<div className="flex items-center justify-between mb-4">
						<h2 className="text-lg font-semibold text-slate-900">Pending User Verifications</h2>
						<span className="text-sm text-slate-500">Review ID images to approve or reject users</span>
					</div>

					{fetchLoading ? (
						<div className="rounded-xl border border-slate-200 bg-slate-50 p-8 text-center">
							<div className="text-violet-600">Loading pending verifications...</div>
						</div>
					) : pendingUsers.length === 0 ? (
						<div className="rounded-xl border border-slate-200 bg-slate-50 p-8 text-center">
							<div className="text-4xl mb-3">🪪</div>
							<p className="text-slate-600 font-medium">No pending verifications</p>
							<p className="text-sm text-slate-500 mt-1">New client and technician registrations requiring ID verification will appear here.</p>
						</div>
					) : (
						<div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
							{pendingUsers.map((pendingUser) => (
								<div key={pendingUser.id} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
									<div className="flex items-start gap-3">
										<div className="flex h-10 w-10 items-center justify-center rounded-full bg-violet-100 text-lg">
											{pendingUser.role === 'technician' ? '👥' : '👤'}
										</div>
										<div className="flex-1 min-w-0">
											<p className="font-medium text-slate-900 truncate">{pendingUser.name}</p>
											<p className="text-xs text-slate-500">{pendingUser.email}</p>
											<span className={`inline-flex mt-1 px-2 py-0.5 rounded-full text-xs font-medium ${
												pendingUser.role === 'technician' 
													? 'bg-orange-100 text-orange-700' 
													: 'bg-blue-100 text-blue-700'
											}`}>
												{pendingUser.role}
											</span>
										</div>
									</div>
									<button
										onClick={() => setSelectedUser(pendingUser)}
										className="mt-3 w-full rounded-lg bg-violet-600 py-2 text-sm font-medium text-white hover:bg-violet-700 transition-colors"
									>
										Review ID Document
									</button>
								</div>
							))}
						</div>
					)}
				</div>

				{/* ID Verification Modal */}
				{selectedUser && (
					<div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
						<div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl">
							<div className="flex items-center justify-between mb-4">
								<h3 className="text-xl font-bold text-slate-900">Verify User - ID Review</h3>
								<button
									onClick={() => setSelectedUser(null)}
									className="rounded-full p-1 hover:bg-slate-100 text-slate-500"
								>
									✕
								</button>
							</div>

							<div className="space-y-4">
								{/* User Details */}
								<div className="rounded-lg bg-slate-50 p-4">
									<h4 className="font-semibold text-slate-900 mb-2">User Information</h4>
									<div className="grid grid-cols-2 gap-4 text-sm">
										<div>
											<p className="text-slate-500">Name</p>
											<p className="font-medium">{selectedUser.name}</p>
										</div>
										<div>
											<p className="text-slate-500">Email</p>
											<p className="font-medium">{selectedUser.email}</p>
										</div>
										<div>
											<p className="text-slate-500">Contact</p>
											<p className="font-medium">{selectedUser.contactNo || 'N/A'}</p>
										</div>
										<div>
											<p className="text-slate-500">Role</p>
											<span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
												selectedUser.role === 'technician' 
													? 'bg-orange-100 text-orange-700' 
													: 'bg-blue-100 text-blue-700'
											}`}>
												{selectedUser.role}
											</span>
										</div>
									</div>
									<p className="text-xs text-slate-400 mt-2">
										Registered: {new Date(selectedUser.createdAt).toLocaleString()}
									</p>
								</div>

								{/* ID Image */}
								<div>
									<h4 className="font-semibold text-slate-900 mb-2">Submitted ID Document</h4>
									{/* Debug info */}
									{selectedUser.imageUrl && (
										<div className="mb-2 rounded bg-slate-100 p-2 text-xs">
											<span className="font-mono text-slate-600">Image URL: {selectedUser.imageUrl}</span>
										</div>
									)}
									{selectedUser.imageUrl ? (
										<div className="rounded-lg border border-slate-200 overflow-hidden bg-slate-50">
											<img
												src={selectedUser.imageUrl}
												alt="User ID Document"
												className="w-full h-auto max-h-96 object-contain"
												onError={(e) => {
													// If image fails to load, show error message
													const target = e.target as HTMLImageElement;
													target.style.display = 'none';
													const parent = target.parentElement;
													if (parent) {
														parent.innerHTML = `
															<div class="p-8 text-center">
																<p class="text-slate-500 mb-2">⚠️ Unable to load image</p>
																<p class="text-xs text-slate-400">URL: ${selectedUser.imageUrl}</p>
																<p class="text-xs text-slate-400 mt-2">The image may be stored in Supabase storage. Please check the URL or upload a new image.</p>
															</div>
														`;
													}
												}}
											/>
										</div>
									) : (
										<div className="rounded-lg border border-slate-200 bg-slate-50 p-8 text-center">
											<p className="text-slate-500">No ID image submitted</p>
											<p className="text-xs text-slate-400 mt-1">User registered without uploading an ID document</p>
										</div>
									)}
								</div>

								{/* Action Buttons */}
								<div className="flex gap-3 pt-4 border-t border-slate-200">
									<button
										onClick={() => handleReject(selectedUser.id)}
										disabled={loading}
										className="flex-1 rounded-lg bg-rose-100 py-3 text-sm font-semibold text-rose-700 hover:bg-rose-200 disabled:opacity-50 transition-colors"
									>
										{loading ? 'Processing...' : '❌ Reject'}
									</button>
									<button
										onClick={() => handleApprove(selectedUser.id)}
										disabled={loading}
										className="flex-1 rounded-lg bg-emerald-100 py-3 text-sm font-semibold text-emerald-700 hover:bg-emerald-200 disabled:opacity-50 transition-colors"
									>
										{loading ? 'Processing...' : '✓ Approve'}
									</button>
								</div>
							</div>
						</div>
					</div>
				)}

				{/* Booking Details Modal */}
				<BookingDetailsModal
					isOpen={modalOpen}
					onClose={handleModalClose}
					title={modalTitle}
					statuses={modalStatuses}
					initialCount={modalInitialCount}
				/>
			</main>
		</div>
	);
}
