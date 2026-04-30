import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
	LogOut, 
	CheckCircle, 
	Clock, 
	MapPin, 
	Calendar, 
	DollarSign, 
	AlertCircle,
	Briefcase,
	TrendingUp,
	Power,
	Eye,
	X,
	ChevronRight,
	Check,
	Camera,
	Upload,
	ImageIcon,
	ListChecks
} from 'lucide-react';
import api from '../../api/axios';

// Types
interface User {
	id: string;
	name: string;
	email: string;
	contactNo?: string;
	verified: boolean;
	role: string;
}

interface Booking {
	id: string;
	bookingCode: string;
	serviceType: string;
	deviceType: string;
	addOns?: string[];
	timeSlot: string;
	bookingDate: string;
	address?: string;
	landmark?: string;
	specialInstructions?: string;
	totalAmount: number;
	status: 'pending' | 'confirmed' | 'in_progress' | 'completed' | 'cancelled' | 'no_show';
	createdAt: string;
	confirmedAt?: string;
	startedAt?: string;
	completedAt?: string;
	clientName?: string;
	clientContact?: string;
	clientEmail?: string;
	technicianAssigned?: boolean;
	acceptanceMessage?: string;
	statusMessage?: string;
}

export default function Tdashboard() {
	const navigate = useNavigate();
	
	// User state
	const [user, setUser] = useState<User | null>(null);
	const [loading, setLoading] = useState(true);
	
	// Data states
	const [pendingBookings, setPendingBookings] = useState<Booking[]>([]);
	const [myBookings, setMyBookings] = useState<Booking[]>([]);
	const [isAvailable, setIsAvailable] = useState<boolean>(true);
	
	// UI states
	const [activeTab, setActiveTab] = useState<'overview' | 'pending' | 'my-bookings'>('overview');
	const [loadingPending, setLoadingPending] = useState(false);
	const [loadingMyBookings, setLoadingMyBookings] = useState(false);
	const [acceptingBooking, setAcceptingBooking] = useState<string | null>(null);
	const [updatingStatus, setUpdatingStatus] = useState<string | null>(null);
	const [togglingAvailability, setTogglingAvailability] = useState(false);
	const [selectedBooking, setSelectedBooking] = useState<Booking | null>(null);
	const [showBookingModal, setShowBookingModal] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [successMessage, setSuccessMessage] = useState<string | null>(null);
	
	// Checklist state
	const [checklist, setChecklist] = useState<Array<{id: string, label: string, isChecked: boolean, checkedAt?: string}>>([]);
	const [loadingChecklist, setLoadingChecklist] = useState(false);
	const [updatingChecklist, setUpdatingChecklist] = useState<string | null>(null);
	
	// Photo state
	const [photos, setPhotos] = useState<Array<{id: string, type: string, fileUrl: string, uploadedAt: string}>>([]);
	const [loadingPhotos, setLoadingPhotos] = useState(false);
	const [uploadingPhoto, setUploadingPhoto] = useState(false);
	const [selectedBeforeFiles, setSelectedBeforeFiles] = useState<FileList | null>(null);
	const [selectedAfterFiles, setSelectedAfterFiles] = useState<FileList | null>(null);

	// Load user from localStorage and fetch fresh profile
	useEffect(() => {
		const loadUser = async () => {
			try {
				const raw = localStorage.getItem('cleanit.user');
				if (!raw) {
					navigate('/login');
					return;
				}
				
				const storedUser = JSON.parse(raw);
				
				// Fetch fresh user data including verification status
				try {
					const response = await api.get(`/v1/user/profile/${storedUser.email}`);
					const updatedUser = { ...storedUser, ...response.data };
					localStorage.setItem('cleanit.user', JSON.stringify(updatedUser));
					setUser(updatedUser);
					console.log('Technician user loaded:', updatedUser.name, '| Verified:', updatedUser.verified);
				} catch (err) {
					console.error('Failed to fetch user profile, using stored data');
					setUser(storedUser);
				}
			} catch (err) {
				console.error('Failed to parse user');
				navigate('/login');
			} finally {
				setLoading(false);
			}
		};
		
		loadUser();
	}, [navigate]);

	// Fetch all data when user is loaded and verified
	useEffect(() => {
		if (user?.id) {
			console.log('Checking technician verification - ID:', user.id, '| Verified:', user.verified);
			if (user.verified) {
				console.log('Technician is verified, fetching data...');
				fetchAllData();
			} else {
				console.warn('Technician is NOT verified - waiting for admin approval');
				setError('Your account is pending admin verification. Please wait before accessing bookings.');
			}
		}
	}, [user?.id, user?.verified]);

	const fetchAllData = async () => {
		if (!user?.id) return;
		
		await Promise.all([
			fetchPendingBookings(),
			fetchMyBookings(),
			fetchAvailability()
		]);
	};

	const fetchPendingBookings = async () => {
		if (!user?.id) {
			console.warn('Cannot fetch pending bookings: user ID not available');
			return;
		}
		
		setLoadingPending(true);
		try {
			// Pass authenticated technician's ID to backend
			const response = await api.get(`/v1/technician/bookings/pending?technicianId=${user.id}`);
			console.log('Pending bookings response:', response.data);
			console.log('Number of pending bookings:', response.data.length);
			setPendingBookings(response.data);
		} catch (err: any) {
			console.error('Failed to fetch pending bookings:', err);
			console.error('Error response:', err.response?.data);
			setError('Failed to load pending bookings: ' + (err.response?.data?.message || err.message));
		} finally {
			setLoadingPending(false);
		}
	};

	const fetchMyBookings = async () => {
		if (!user?.id) return;
		setLoadingMyBookings(true);
		try {
			const response = await api.get(`/v1/technician/${user.id}/bookings`);
			setMyBookings(response.data);
		} catch (err: any) {
			console.error('Failed to fetch my bookings:', err);
		} finally {
			setLoadingMyBookings(false);
		}
	};

	const fetchAvailability = async () => {
		if (!user?.id) return;
		try {
			const response = await api.get(`/v1/technician/${user.id}/availability`);
			setIsAvailable(response.data.isAvailable);
		} catch (err: any) {
			console.error('Failed to fetch availability:', err);
		}
	};

	const handleAcceptBooking = async (bookingId: string) => {
		if (!user?.id) return;
		setAcceptingBooking(bookingId);
		setError(null);
		
		try {
			await api.post(`/v1/technician/bookings/${bookingId}/accept`, {
				technicianId: user.id
			});
			
			setSuccessMessage('Booking accepted successfully!');
			setTimeout(() => setSuccessMessage(null), 3000);
			
			// Refresh data
			await Promise.all([
				fetchPendingBookings(),
				fetchMyBookings()
			]);
		} catch (err: any) {
			setError(err?.response?.data?.message || 'Failed to accept booking');
			setTimeout(() => setError(null), 5000);
		} finally {
			setAcceptingBooking(null);
		}
	};

	const handleUpdateStatus = async (bookingId: string, newStatus: string) => {
		if (!user?.id) return;
		
		// AC-11 FIX: Validate checklist before In Progress -> Completed
		// Checklist is initialized when service starts, validated when completing
		if (newStatus === 'completed') {
			try {
				const checklistResponse = await api.get(`/v1/technician/bookings/${bookingId}/validate-checklist`);
				if (!checklistResponse.data.isComplete) {
					const incompleteItems = checklistResponse.data.incompleteItems || [];
					setError(
						`Cannot complete service. ${incompleteItems.length} checklist item(s) incomplete:\n` +
						incompleteItems.slice(0, 3).join(', ') +
						(incompleteItems.length > 3 ? '...' : '')
					);
					setTimeout(() => setError(null), 8000);
					return;
				}
			} catch (err: any) {
				setError('Failed to validate checklist');
				setTimeout(() => setError(null), 5000);
				return;
			}
		}

		// AC-12: Validate photos before In Progress -> Completed
		if (newStatus === 'completed') {
			try {
				const validationResponse = await api.get(`/v1/technician/bookings/${bookingId}/validate-photos`);
				if (!validationResponse.data.hasRequiredPhotos) {
					const missingRequirements = validationResponse.data.missingRequirements || [];
					setError(
						`Cannot complete service. Photo requirements not met:\n` +
						missingRequirements.join('\n')
					);
					setTimeout(() => setError(null), 8000);
					return;
				}
			} catch (err: any) {
				setError('Failed to validate photos');
				setTimeout(() => setError(null), 5000);
				return;
			}
		}

		setUpdatingStatus(bookingId);
		setError(null);
		
		try {
			await api.post(`/v1/technician/bookings/${bookingId}/status`, {
				status: newStatus,
				technicianId: user.id,
				reason: `Status updated to ${newStatus}`
			});
			
			setSuccessMessage(`Booking marked as ${newStatus.replace('_', ' ')}`);
			setTimeout(() => setSuccessMessage(null), 3000);
			
			// Refresh data
			await fetchMyBookings();
		} catch (err: any) {
			// Display backend validation error
			const errorMessage = err?.response?.data?.message || 'Failed to update status';
			setError(errorMessage);
			setTimeout(() => setError(null), 8000);
		} finally {
			setUpdatingStatus(null);
		}
	};

	const handleToggleAvailability = async () => {
		if (!user?.id) return;
		setTogglingAvailability(true);
		setError(null);
		
		try {
			await api.post(`/v1/technician/${user.id}/availability`, {
				isAvailable: !isAvailable
			});
			
			setIsAvailable(!isAvailable);
			setSuccessMessage(`You are now ${!isAvailable ? 'available' : 'unavailable'} for new bookings`);
			setTimeout(() => setSuccessMessage(null), 3000);
		} catch (err: any) {
			setError(err?.response?.data?.message || 'Failed to update availability');
			setTimeout(() => setError(null), 5000);
		} finally {
			setTogglingAvailability(false);
		}
	};

	const handleLogout = () => {
		localStorage.removeItem('cleanit.user');
		localStorage.removeItem('cleanit.token');
		navigate('/login');
	};

	const openBookingDetails = async (booking: Booking) => {
		setSelectedBooking(booking);
		setShowBookingModal(true);
		
		// Fetch checklist and photos for this booking
		await Promise.all([
			fetchChecklist(booking.id),
			fetchPhotos(booking.id)
		]);
	};

	const fetchChecklist = async (bookingId: string) => {
		setLoadingChecklist(true);
		try {
			const response = await api.get(`/v1/technician/bookings/${bookingId}/checklist`);
			setChecklist(response.data);
		} catch (err: any) {
			console.error('Failed to fetch checklist:', err);
			setChecklist([]);
		} finally {
			setLoadingChecklist(false);
		}
	};

	const toggleChecklistItem = async (checklistItemId: string) => {
		if (!selectedBooking || !user?.id) return;
		
		setUpdatingChecklist(checklistItemId);
		try {
			await api.post(
				`/v1/technician/bookings/${selectedBooking.id}/checklist/${checklistItemId}`,
				{ technicianId: user.id }
			);
			
			// Refresh checklist
			await fetchChecklist(selectedBooking.id);
			
			setSuccessMessage('Checklist item updated');
			setTimeout(() => setSuccessMessage(null), 2000);
		} catch (err: any) {
			setError(err?.response?.data?.message || 'Failed to update checklist');
			setTimeout(() => setError(null), 5000);
		} finally {
			setUpdatingChecklist(null);
		}
	};

	const fetchPhotos = async (bookingId: string) => {
		setLoadingPhotos(true);
		try {
			const response = await api.get(`/v1/technician/bookings/${bookingId}/photos`);
			setPhotos(response.data);
		} catch (err: any) {
			console.error('Failed to fetch photos:', err);
			setPhotos([]);
		} finally {
			setLoadingPhotos(false);
		}
	};

	const handlePhotoUpload = async (photoType: 'BEFORE' | 'AFTER') => {
		if (!selectedBooking) return;
		
		const files = photoType === 'BEFORE' ? selectedBeforeFiles : selectedAfterFiles;
		if (!files || files.length === 0) {
			setError('Please select photos to upload');
			return;
		}

		setUploadingPhoto(true);
		setError(null);

		try {
			// Upload each file
			for (let i = 0; i < files.length; i++) {
				const formData = new FormData();
				formData.append('file', files[i]);
				formData.append('bookingId', selectedBooking.id);
				formData.append('type', photoType);
				formData.append('technicianId', user?.id || '');

				await api.post(`/v1/technician/bookings/${selectedBooking.id}/photos`, formData, {
					headers: {
						'Content-Type': 'multipart/form-data'
					}
				});
			}

			setSuccessMessage(`${files.length} photo(s) uploaded successfully`);
			setTimeout(() => setSuccessMessage(null), 3000);
			
			// Clear selected files
			if (photoType === 'BEFORE') {
				setSelectedBeforeFiles(null);
			} else {
				setSelectedAfterFiles(null);
			}

			// Refresh photos
			await fetchPhotos(selectedBooking.id);
		} catch (err: any) {
			setError(err?.response?.data?.message || 'Failed to upload photos');
			setTimeout(() => setError(null), 5000);
		} finally {
			setUploadingPhoto(false);
		}
	};

	const getChecklistCompletion = () => {
		if (checklist.length === 0) return 0;
		const checked = checklist.filter(item => item.isChecked).length;
		return Math.round((checked / checklist.length) * 100);
	};

	const getPhotoCount = (type: 'BEFORE' | 'AFTER') => {
		return photos.filter(p => p.type === type).length;
	};

	const getStatusColor = (status: string) => {
		switch (status) {
			case 'pending': return 'bg-orange-100 text-orange-700 border-orange-200';
			case 'confirmed': return 'bg-emerald-100 text-emerald-700 border-emerald-200';
			case 'in_progress': return 'bg-blue-100 text-blue-700 border-blue-200';
			case 'completed': return 'bg-violet-100 text-violet-700 border-violet-200';
			case 'cancelled': return 'bg-rose-100 text-rose-700 border-rose-200';
			case 'no_show': return 'bg-gray-100 text-gray-700 border-gray-200';
			default: return 'bg-slate-100 text-slate-700 border-slate-200';
		}
	};

	const getStatusLabel = (status: string) => {
		return status.replace('_', ' ').replace(/\b\w/g, l => l.toUpperCase());
	};

	const canStartService = (booking: Booking) => booking.status === 'confirmed';
	const canCompleteService = (booking: Booking) => booking.status === 'in_progress';
	const canMarkNoShow = (booking: Booking) => booking.status === 'confirmed';

	// Get active and upcoming bookings
	const activeBooking = myBookings.find(b => b.status === 'in_progress');
	const upcomingBooking = myBookings.find(b => b.status === 'confirmed');

	if (loading) {
		return (
			<div className="min-h-screen bg-slate-50 flex items-center justify-center">
				<div className="text-violet-600">Loading...</div>
			</div>
		);
	}

	if (!user) return null;

	return (
		<div className="min-h-screen bg-slate-50">
			<div className="mx-auto max-w-7xl px-4 py-8">
				{/* Header Section */}
				<div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
					<div className="flex flex-col gap-2">
						<div className="text-xs font-semibold uppercase tracking-wider text-slate-500">Technician Dashboard</div>
						<h1 className="text-3xl font-extrabold text-slate-900">Welcome Back, {user?.name || 'Technician'}!</h1>
						<div className="text-sm text-slate-500">Manage your bookings and requests</div>
					</div>

					{/* Profile Card */}
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
						
						{/* Availability Toggle */}
						{user?.verified && (
							<div className="mt-4 flex items-center justify-between rounded-lg bg-slate-50 p-3">
								<div className="flex items-center gap-2">
									<Power className={`h-4 w-4 ${isAvailable ? 'text-emerald-600' : 'text-slate-400'}`} />
									<span className="text-sm font-medium text-slate-700">
										{isAvailable ? 'Available' : 'Unavailable'}
									</span>
								</div>
								<button
									onClick={handleToggleAvailability}
									disabled={togglingAvailability}
									className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
										isAvailable ? 'bg-emerald-500' : 'bg-slate-300'
									} ${togglingAvailability ? 'opacity-60' : ''}`}
								>
									<span
										className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
											isAvailable ? 'translate-x-6' : 'translate-x-1'
										}`}
									/>
								</button>
							</div>
						)}
						
						<div className="mt-3 flex gap-2">
							<button
								onClick={handleLogout}
								className="flex-1 rounded-lg border border-rose-300 bg-rose-50 px-3 py-2 text-xs font-semibold text-rose-700 hover:bg-rose-100 transition-colors flex items-center justify-center gap-1"
							>
								<LogOut className="h-3 w-3" />
								Logout
							</button>
						</div>
					</div>
				</div>

				{/* Alerts */}
				{error && (
					<div className="mt-6 rounded-lg border border-rose-200 bg-rose-50 p-4 flex items-center gap-3">
						<AlertCircle className="h-5 w-5 text-rose-600 flex-shrink-0" />
						<p className="text-sm text-rose-700">{error}</p>
					</div>
				)}
				
				{successMessage && (
					<div className="mt-6 rounded-lg border border-emerald-200 bg-emerald-50 p-4 flex items-center gap-3">
						<CheckCircle className="h-5 w-5 text-emerald-600 flex-shrink-0" />
						<p className="text-sm text-emerald-700">{successMessage}</p>
					</div>
				)}

				{/* Verification Warning */}
				{!user?.verified && (
					<div className="mt-6 rounded-2xl border border-rose-200 bg-rose-50 p-6 shadow-sm">
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
						</div>
					</div>
				)}

				{/* Navigation Tabs */}
				{user?.verified && (
					<div className="mt-8 border-b border-slate-200">
						<div className="flex gap-1">
							{[
								{ id: 'overview', label: 'Overview', icon: TrendingUp },
								{ id: 'pending', label: 'Pending Requests', icon: Briefcase },
								{ id: 'my-bookings', label: 'My Bookings', icon: CheckCircle }
							].map(({ id, label, icon: Icon }) => (
								<button
									key={id}
									onClick={() => setActiveTab(id as any)}
									className={`flex items-center gap-2 px-4 py-3 text-sm font-medium border-b-2 transition-colors ${
										activeTab === id
											? 'border-violet-500 text-violet-700'
											: 'border-transparent text-slate-500 hover:text-slate-700'
									}`}
								>
									<Icon className="h-4 w-4" />
									{label}
									{id === 'pending' && pendingBookings.length > 0 && (
										<span className="ml-1 rounded-full bg-violet-500 px-2 py-0.5 text-xs text-white">
											{pendingBookings.length}
										</span>
									)}
								</button>
							))}
						</div>
					</div>
				)}

				{/* Content Sections */}
				{user?.verified && (
					<div className="mt-6">
						{/* Overview Tab */}
						{activeTab === 'overview' && (
							<div className="space-y-6">
								{/* Quick Actions */}
								<div className="grid grid-cols-1 gap-4 md:grid-cols-2">
									{/* Recent Pending Bookings */}
									<div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
										<div className="flex items-center justify-between mb-4">
											<h3 className="text-sm font-bold text-slate-900">Pending Requests</h3>
											<button
												onClick={() => setActiveTab('pending')}
												className="text-xs text-violet-600 hover:text-violet-700 font-medium flex items-center gap-1"
											>
												View All
												<ChevronRight className="h-3 w-3" />
											</button>
										</div>
										
										{loadingPending ? (
											<div className="text-center py-8 text-slate-500">Loading...</div>
										) : pendingBookings.length === 0 ? (
											<div className="text-center py-8">
												<div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-xl mb-3">📋</div>
												<p className="text-sm text-slate-500">No pending requests</p>
											</div>
										) : (
											<div className="space-y-3">
												{pendingBookings.slice(0, 3).map(booking => (
													<div key={booking.id} className="rounded-lg border border-slate-200 p-3">
														<div className="flex items-center justify-between">
															<div>
																<p className="text-sm font-medium text-slate-900">{booking.serviceType}</p>
																<p className="text-xs text-slate-500">{booking.bookingDate} • {booking.timeSlot}</p>
															</div>
															<button
																onClick={() => handleAcceptBooking(booking.id)}
																disabled={acceptingBooking === booking.id}
																className="rounded-lg bg-violet-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-violet-700 disabled:opacity-60"
															>
																{acceptingBooking === booking.id ? 'Accepting...' : 'Accept'}
															</button>
														</div>
													</div>
												))}
											</div>
										)}
									</div>

									{/* Active Bookings */}
									<div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
										<div className="flex items-center justify-between mb-4">
											<h3 className="text-sm font-bold text-slate-900">Active Bookings</h3>
											<button
												onClick={() => setActiveTab('my-bookings')}
												className="text-xs text-violet-600 hover:text-violet-700 font-medium flex items-center gap-1"
											>
												View All
												<ChevronRight className="h-3 w-3" />
											</button>
										</div>
										
										{loadingMyBookings ? (
											<div className="text-center py-8 text-slate-500">Loading...</div>
										) : myBookings.filter(b => ['confirmed', 'in_progress'].includes(b.status)).length === 0 ? (
											<div className="text-center py-8">
												<div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-xl mb-3">🕒</div>
												<p className="text-sm text-slate-500">No active bookings</p>
											</div>
										) : (
											<div className="space-y-3">
												{myBookings
													.filter(b => ['confirmed', 'in_progress'].includes(b.status))
													.slice(0, 3)
													.map(booking => (
														<div key={booking.id} className="rounded-lg border border-slate-200 p-3">
															<div className="flex items-center justify-between">
																<div>
																	<p className="text-sm font-medium text-slate-900">{booking.serviceType}</p>
																	<span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${getStatusColor(booking.status)}`}>
																		{getStatusLabel(booking.status)}
																	</span>
																</div>
																<button
																	onClick={() => openBookingDetails(booking)}
																	className="rounded-lg bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-200"
																>
																	View
																</button>
															</div>
														</div>
												))}
											</div>
										)}
									</div>
								</div>
							</div>
						)}

						{/* Pending Requests Tab */}
						{activeTab === 'pending' && (
							<div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
								<h2 className="text-lg font-bold text-slate-900 mb-4">Pending Service Requests</h2>
								
								{loadingPending ? (
									<div className="text-center py-12">
										<div className="text-slate-500">Loading pending requests...</div>
									</div>
								) : pendingBookings.length === 0 ? (
									<div className="text-center py-12">
										<div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-slate-100 text-3xl mb-4">📋</div>
										<h3 className="text-sm font-semibold text-slate-700">No Pending Requests</h3>
										<p className="mt-1 text-xs text-slate-500 max-w-sm mx-auto">
											There are no service requests waiting at the moment. Check back soon!
										</p>
									</div>
								) : (
									<div className="space-y-4">
										{pendingBookings.map(booking => (
											<div key={booking.id} className="rounded-xl border border-slate-200 p-4 hover:shadow-md transition-shadow">
												<div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
													<div className="flex-1">
														<div className="flex items-center gap-2 mb-2">
															<span className="text-xs font-mono text-slate-500">#{booking.bookingCode}</span>
															<span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${getStatusColor(booking.status)}`}>
																Pending
															</span>
														</div>
														<h3 className="text-sm font-semibold text-slate-900">{booking.serviceType}</h3>
														<p className="text-xs text-slate-500 mt-1">Device: {booking.deviceType}</p>
														
														{/* Client Information */}
														{booking.clientName && (
															<div className="mt-3 p-2 rounded-lg bg-blue-50 border border-blue-100">
																<p className="text-xs font-medium text-blue-900">Client Details</p>
																<p className="text-xs text-blue-700 mt-1">👤 {booking.clientName}</p>
																{booking.clientContact && (
																	<p className="text-xs text-blue-700">📱 {booking.clientContact}</p>
																)}
																{booking.clientEmail && (
																	<p className="text-xs text-blue-700">✉️ {booking.clientEmail}</p>
																)}
															</div>
														)}
														
														<div className="flex flex-wrap gap-3 mt-3 text-xs text-slate-500">
															<span className="flex items-center gap-1">
																<Calendar className="h-3 w-3" />
																{booking.bookingDate}
															</span>
															<span className="flex items-center gap-1">
																<Clock className="h-3 w-3" />
																{booking.timeSlot}
															</span>
															<span className="flex items-center gap-1">
																<MapPin className="h-3 w-3" />
																Address hidden until accepted
															</span>
														</div>
														
														{booking.addOns && booking.addOns.length > 0 && (
															<div className="mt-2">
																<span className="text-xs text-slate-500">Add-ons: </span>
																{booking.addOns.map((addon, idx) => (
																	<span key={idx} className="text-xs text-violet-600">{addon}{idx < booking.addOns!.length - 1 ? ', ' : ''}</span>
																))}
															</div>
														)}
													</div>
													
													<div className="flex flex-col gap-2 sm:items-end">
														<div className="text-lg font-bold text-violet-600">₱{booking.totalAmount.toLocaleString()}</div>
														<button
															onClick={() => handleAcceptBooking(booking.id)}
															disabled={acceptingBooking === booking.id}
															className="rounded-lg bg-violet-600 px-4 py-2 text-sm font-semibold text-white hover:bg-violet-700 transition-colors disabled:opacity-60 flex items-center justify-center gap-2"
														>
															{acceptingBooking === booking.id ? (
																<>
																	<div className="h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
																	Accepting...
																</>
															) : (
																<>
																	<Check className="h-4 w-4" />
																	Accept Booking
																</>
															)}
														</button>
													</div>
												</div>
											</div>
										))}
									</div>
								)}
							</div>
						)}

						{/* My Bookings Tab */}
						{activeTab === 'my-bookings' && (
							<div className="space-y-6">
								{/* Active Bookings */}
								<div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
									<h2 className="text-lg font-bold text-slate-900 mb-4">Active Bookings</h2>
									
									{loadingMyBookings ? (
										<div className="text-center py-8 text-slate-500">Loading...</div>
									) : myBookings.filter(b => ['confirmed', 'in_progress'].includes(b.status)).length === 0 ? (
										<div className="text-center py-8">
											<div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-slate-100 text-2xl mb-3">🕒</div>
											<p className="text-sm text-slate-500">No active bookings</p>
										</div>
									) : (
										<div className="space-y-4">
											{myBookings
												.filter(b => ['confirmed', 'in_progress'].includes(b.status))
												.map(booking => (
													<div key={booking.id} className="rounded-xl border border-slate-200 p-4">
														<div className="flex flex-col gap-4">
															<div className="flex items-start justify-between">
																<div>
																	<div className="flex items-center gap-2">
																		<span className="text-xs font-mono text-slate-500">#{booking.bookingCode}</span>
																		<span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${getStatusColor(booking.status)}`}>
																			{getStatusLabel(booking.status)}
																		</span>
																	</div>
																	<h3 className="text-sm font-semibold text-slate-900 mt-1">{booking.serviceType}</h3>
																	<p className="text-xs text-slate-500">{booking.deviceType}</p>
																</div>
																<div className="text-lg font-bold text-violet-600">₱{booking.totalAmount.toLocaleString()}</div>
															</div>
															
															<div className="flex flex-wrap gap-2 text-xs text-slate-500">
																<span className="flex items-center gap-1"><Calendar className="h-3 w-3" /> {booking.bookingDate}</span>
																<span className="flex items-center gap-1"><Clock className="h-3 w-3" /> {booking.timeSlot}</span>
																{booking.address && <span className="flex items-center gap-1"><MapPin className="h-3 w-3" /> {booking.address}</span>}
															</div>
															
															{/* Client Info */}
															{booking.clientName && (
																<div className="rounded-lg bg-slate-50 p-3">
																	<p className="text-xs text-slate-500">Client</p>
																	<p className="text-sm font-medium text-slate-900">{booking.clientName}</p>
																	{booking.clientContact && <p className="text-xs text-slate-500">{booking.clientContact}</p>}
																</div>
															)}
															
															{/* Action Buttons */}
															<div className="flex flex-wrap gap-2">
																{canStartService(booking) && (
																	<button
																		onClick={() => handleUpdateStatus(booking.id, 'in_progress')}
																		disabled={updatingStatus === booking.id}
																		className="rounded-lg bg-blue-600 px-3 py-2 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-60 flex items-center gap-1"
																	>
																		{updatingStatus === booking.id ? 'Updating...' : 'Start Service'}
																	</button>
																)}
																{canCompleteService(booking) && (
																	<button
																		onClick={() => handleUpdateStatus(booking.id, 'completed')}
																		disabled={updatingStatus === booking.id}
																		className="rounded-lg bg-emerald-600 px-3 py-2 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-60 flex items-center gap-1"
																	>
																		{updatingStatus === booking.id ? 'Updating...' : 'Mark Complete'}
																	</button>
																)}
																{canMarkNoShow(booking) && (
																	<button
																		onClick={() => handleUpdateStatus(booking.id, 'no_show')}
																		disabled={updatingStatus === booking.id}
																		className="rounded-lg bg-gray-500 px-3 py-2 text-xs font-semibold text-white hover:bg-gray-600 disabled:opacity-60"
																	>
																		{updatingStatus === booking.id ? 'Updating...' : 'No Show'}
																	</button>
																)}
																<button
																	onClick={() => openBookingDetails(booking)}
																	className="rounded-lg bg-slate-100 px-3 py-2 text-xs font-medium text-slate-700 hover:bg-slate-200 flex items-center gap-1"
																>
																	<Eye className="h-3 w-3" />
																	View Details
																</button>
															</div>
														</div>
													</div>
												))}
										</div>
									)}
								</div>

								{/* Booking History */}
								<div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
									<h2 className="text-lg font-bold text-slate-900 mb-4">Booking History</h2>
									
									{loadingMyBookings ? (
										<div className="text-center py-8 text-slate-500">Loading...</div>
									) : myBookings.filter(b => ['completed', 'cancelled', 'no_show'].includes(b.status)).length === 0 ? (
										<div className="text-center py-8">
											<div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-slate-100 text-2xl mb-3">📅</div>
											<p className="text-sm text-slate-500">No booking history</p>
										</div>
									) : (
										<div className="space-y-3">
											{myBookings
												.filter(b => ['completed', 'cancelled', 'no_show'].includes(b.status))
												.map(booking => (
													<div key={booking.id} className="rounded-lg border border-slate-200 p-4 opacity-75">
														<div className="flex items-center justify-between">
															<div>
																<div className="flex items-center gap-2">
																	<span className="text-xs font-mono text-slate-500">#{booking.bookingCode}</span>
																	<span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${getStatusColor(booking.status)}`}>
																		{getStatusLabel(booking.status)}
																	</span>
																</div>
																<h3 className="text-sm font-semibold text-slate-900 mt-1">{booking.serviceType}</h3>
																<p className="text-xs text-slate-500">{booking.bookingDate}</p>
															</div>
															<div className="text-right">
																<div className="text-sm font-bold text-slate-900">₱{booking.totalAmount.toLocaleString()}</div>
																<button
																		onClick={() => openBookingDetails(booking)}
																		className="mt-1 text-xs text-violet-600 hover:text-violet-700"
																	>
																		View Details
																	</button>
															</div>
														</div>
													</div>
												))}
										</div>
									)}
								</div>
							</div>
						)}
					</div>
				)}
			</div>

			{/* Booking Details Modal */}
			{showBookingModal && selectedBooking && (
				<div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
					<div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-6 shadow-xl">
						<div className="mb-4 flex items-center justify-between sticky top-0 bg-white pb-4 border-b border-slate-200">
							<h3 className="text-lg font-bold text-slate-900">Booking Details</h3>
							<button
								onClick={() => setShowBookingModal(false)}
								className="rounded-full p-1 hover:bg-slate-100"
							>
								<X className="h-5 w-5 text-slate-500" />
							</button>
						</div>
						
						{/* Error/Success Messages */}
						{error && (
							<div className="mb-4 rounded-lg border border-rose-200 bg-rose-50 p-3 flex items-start gap-2">
								<AlertCircle className="h-4 w-4 text-rose-600 flex-shrink-0 mt-0.5" />
								<p className="text-xs text-rose-700 whitespace-pre-line">{error}</p>
							</div>
						)}
						{successMessage && (
							<div className="mb-4 rounded-lg border border-emerald-200 bg-emerald-50 p-3 flex items-start gap-2">
								<CheckCircle className="h-4 w-4 text-emerald-600 flex-shrink-0 mt-0.5" />
								<p className="text-xs text-emerald-700">{successMessage}</p>
							</div>
						)}
						
						<div className="space-y-4">
							<div className="rounded-lg bg-violet-50 p-3">
								<div className="text-xs text-slate-500">Booking Code</div>
								<div className="font-mono font-semibold text-slate-900">#{selectedBooking.bookingCode}</div>
							</div>
							
							<div className="grid grid-cols-2 gap-3">
								<div className="rounded-lg bg-slate-50 p-3">
									<div className="text-xs text-slate-500">Service</div>
									<div className="font-semibold text-slate-900">{selectedBooking.serviceType}</div>
								</div>
								<div className="rounded-lg bg-slate-50 p-3">
									<div className="text-xs text-slate-500">Device</div>
									<div className="font-semibold text-slate-900 capitalize">{selectedBooking.deviceType}</div>
								</div>
							</div>
							
							<div className="flex items-center gap-2">
								<span className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium ${getStatusColor(selectedBooking.status)}`}>
									{getStatusLabel(selectedBooking.status)}
								</span>
							</div>
							
							<div className="grid grid-cols-2 gap-3 text-sm">
								<div className="rounded-lg bg-slate-50 p-3">
									<div className="text-xs text-slate-500">Date</div>
									<div className="font-medium text-slate-900">{selectedBooking.bookingDate}</div>
								</div>
								<div className="rounded-lg bg-slate-50 p-3">
									<div className="text-xs text-slate-500">Time</div>
									<div className="font-medium text-slate-900">{selectedBooking.timeSlot}</div>
								</div>
							</div>
							
							{selectedBooking.address && (
								<div className="rounded-lg bg-slate-50 p-3">
									<div className="text-xs text-slate-500">Address</div>
									<div className="font-medium text-slate-900">{selectedBooking.address}</div>
									{selectedBooking.landmark && (
										<div className="text-xs text-slate-500 mt-1">Landmark: {selectedBooking.landmark}</div>
									)}
								</div>
							)}
							
							{selectedBooking.clientName && (
								<div className="rounded-lg bg-slate-50 p-3">
									<div className="text-xs text-slate-500">Client</div>
									<div className="font-semibold text-slate-900">{selectedBooking.clientName}</div>
									{selectedBooking.clientContact && (
										<div className="text-xs text-slate-500">{selectedBooking.clientContact}</div>
									)}
								</div>
							)}
							
							{selectedBooking.addOns && selectedBooking.addOns.length > 0 && (
								<div>
									<div className="text-xs text-slate-500 mb-1">Add-ons</div>
									<div className="flex flex-wrap gap-1">
										{selectedBooking.addOns.map((addon, idx) => (
											<span key={idx} className="rounded-full bg-violet-100 px-2 py-0.5 text-xs text-violet-700">
												{addon}
											</span>
										))}
									</div>
								</div>
							)}
							
							{selectedBooking.specialInstructions && (
								<div className="rounded-lg bg-amber-50 p-3">
									<div className="text-xs text-slate-500">Special Instructions</div>
									<div className="text-sm text-slate-700">{selectedBooking.specialInstructions}</div>
								</div>
							)}
							
							{/* Pre-Service Checklist (AC-11) */}
							{(selectedBooking.status === 'confirmed' || selectedBooking.status === 'in_progress') && (
								<div className="rounded-lg border border-slate-200 p-4">
									<div className="mb-3 flex items-center justify-between">
										<h4 className="text-sm font-semibold text-slate-900 flex items-center gap-2">
											<ListChecks className="h-4 w-4" />
											Pre-Service Checklist
										</h4>
										<span className={`text-xs font-bold ${getChecklistCompletion() === 100 ? 'text-emerald-600' : 'text-slate-600'}`}>
											{getChecklistCompletion()}%
										</span>
									</div>
															
									{/* Progress Bar */}
									<div className="mb-3 h-2 w-full rounded-full bg-slate-200">
										<div 
											className={`h-full rounded-full transition-all ${getChecklistCompletion() === 100 ? 'bg-emerald-500' : 'bg-violet-500'}`}
											style={{ width: `${getChecklistCompletion()}%` }}
										/>
									</div>
							
									{loadingChecklist ? (
										<div className="text-center py-4 text-xs text-slate-500">Loading checklist...</div>
									) : checklist.length === 0 ? (
										<div className="text-center py-4 text-xs text-slate-500">No checklist items found</div>
									) : (
										<div className="space-y-2 max-h-64 overflow-y-auto">
											{checklist.map((item) => (
												<button
													key={item.id}
													onClick={() => toggleChecklistItem(item.id)}
													disabled={updatingChecklist === item.id}
													className={`w-full flex items-center gap-3 p-2 rounded-lg border transition-all ${
														item.isChecked 
															? 'bg-emerald-50 border-emerald-200' 
															: 'bg-white border-slate-200 hover:border-violet-300'
													}`}
												>
													<div className={`flex h-5 w-5 items-center justify-center rounded border ${
														item.isChecked ? 'bg-emerald-500 border-emerald-500' : 'border-slate-300'
													}`}>
														{item.isChecked && <Check className="h-3 w-3 text-white" />}
													</div>
													<span className={`text-xs flex-1 text-left ${item.isChecked ? 'text-emerald-700 line-through' : 'text-slate-700'}`}>
														{item.label}
													</span>
													{item.checkedAt && (
														<span className="text-[10px] text-slate-400">
															{new Date(item.checkedAt).toLocaleTimeString()}
														</span>
													)}
												</button>
											))}
										</div>
									)}
								</div>
							)}
							
							{/* Photo Upload (AC-12) */}
							{(selectedBooking.status === 'in_progress' || selectedBooking.status === 'completed') && (
								<div className="rounded-lg border border-slate-200 p-4">
									<h4 className="text-sm font-semibold text-slate-900 mb-3 flex items-center gap-2">
										<Camera className="h-4 w-4" />
										Photo Documentation
									</h4>
							
									{/* Before Photos */}
									<div className="mb-4">
										<div className="flex items-center justify-between mb-2">
											<span className="text-xs font-medium text-slate-700">Before Service Photos</span>
											<span className="text-xs text-slate-500">{getPhotoCount('BEFORE')} uploaded</span>
										</div>
																
										{/* Photo Preview */}
										{loadingPhotos ? (
											<div className="text-center py-2 text-xs text-slate-500">Loading photos...</div>
										) : (
											<div className="grid grid-cols-3 gap-2 mb-2">
												{photos.filter(p => p.type === 'BEFORE').map((photo) => (
													<div key={photo.id} className="relative aspect-square rounded-lg overflow-hidden bg-slate-100">
														<img src={photo.fileUrl} alt="Before" className="w-full h-full object-cover" />
													</div>
												))}
											</div>
										)}
																
										{/* Upload Input */}
										<div className="flex items-center gap-2">
											<label className="flex-1 flex items-center gap-2 px-3 py-2 rounded-lg border-2 border-dashed border-slate-300 cursor-pointer hover:border-violet-400 transition-all">
												<Upload className="h-4 w-4 text-slate-400" />
												<span className="text-xs text-slate-600">
													{selectedBeforeFiles ? `${selectedBeforeFiles.length} file(s) selected` : 'Select files'}
												</span>
												<input
													type="file"
													accept="image/*"
													multiple
													className="hidden"
													onChange={(e) => setSelectedBeforeFiles(e.target.files)}
												/>
											</label>
											<button
												onClick={() => handlePhotoUpload('BEFORE')}
												disabled={!selectedBeforeFiles || uploadingPhoto}
												className="px-3 py-2 rounded-lg bg-violet-600 text-white text-xs font-medium disabled:opacity-50 disabled:cursor-not-allowed hover:bg-violet-700 transition-all"
											>
												{uploadingPhoto ? 'Uploading...' : 'Upload'}
											</button>
										</div>
									</div>
							
									{/* After Photos */}
									<div>
										<div className="flex items-center justify-between mb-2">
											<span className="text-xs font-medium text-slate-700">After Service Photos</span>
											<span className="text-xs text-slate-500">{getPhotoCount('AFTER')} uploaded</span>
										</div>
																
										{/* Photo Preview */}
										{loadingPhotos ? (
											<div className="text-center py-2 text-xs text-slate-500">Loading photos...</div>
										) : (
											<div className="grid grid-cols-3 gap-2 mb-2">
												{photos.filter(p => p.type === 'AFTER').map((photo) => (
													<div key={photo.id} className="relative aspect-square rounded-lg overflow-hidden bg-slate-100">
														<img src={photo.fileUrl} alt="After" className="w-full h-full object-cover" />
													</div>
												))}
											</div>
										)}
																
										{/* Upload Input */}
										<div className="flex items-center gap-2">
											<label className="flex-1 flex items-center gap-2 px-3 py-2 rounded-lg border-2 border-dashed border-slate-300 cursor-pointer hover:border-violet-400 transition-all">
												<Upload className="h-4 w-4 text-slate-400" />
												<span className="text-xs text-slate-600">
													{selectedAfterFiles ? `${selectedAfterFiles.length} file(s) selected` : 'Select files'}
												</span>
												<input
													type="file"
													accept="image/*"
													multiple
													className="hidden"
													onChange={(e) => setSelectedAfterFiles(e.target.files)}
												/>
											</label>
											<button
												onClick={() => handlePhotoUpload('AFTER')}
												disabled={!selectedAfterFiles || uploadingPhoto}
												className="px-3 py-2 rounded-lg bg-violet-600 text-white text-xs font-medium disabled:opacity-50 disabled:cursor-not-allowed hover:bg-violet-700 transition-all"
											>
												{uploadingPhoto ? 'Uploading...' : 'Upload'}
											</button>
										</div>
									</div>
								</div>
							)}
							
							<div className="rounded-lg bg-violet-50 p-3">
								<div className="text-xs text-slate-500">Total Amount</div>
								<div className="text-xl font-bold text-violet-700">₱{selectedBooking.totalAmount.toLocaleString()}</div>
							</div>
							
							<button
								onClick={() => setShowBookingModal(false)}
								className="w-full rounded-lg bg-violet-600 py-2 text-sm font-semibold text-white hover:bg-violet-700"
				>
					Close
				</button>
						</div>
					</div>
				</div>
			)}
		</div>
	);
}
