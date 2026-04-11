import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Eye } from 'lucide-react';
import api from '../../api/axios';

// Service images - using the images provided by the user
const serviceImages: Record<string, string> = {
	'Standard External Cleaning': '/images/external-cleaning.jpg',
	'Deep Internal Cleaning': '/images/internal-cleaning.jpg',
	'GPU Deep Cleaning': '/images/gpu-cleaning.jpg',
	'PSU Cleaning': '/images/psu-cleaning.jpg',
};

interface Booking {
	id: string;
	serviceType: string;
	deviceType: string;
	addOns: string[];
	timeSlot: string;
	bookingDate: string;
	address: string;
	landmark?: string;
	specialInstructions?: string;
	totalAmount: number;
	status: 'pending' | 'confirmed' | 'in_progress' | 'completed' | 'cancelled';
	technicianName?: string;
	technicianContact?: string;
	createdAt: string;
}

interface Service {
	id: string;
	name: string;
	price: string;
	time: string;
	description: string;
}

const services: Service[] = [
	{
		id: 'external',
		name: 'Standard External Cleaning',
		price: '₱200',
		time: '1-2 hours',
		description: 'Complete external cleaning service'
	},
	{
		id: 'internal',
		name: 'Deep Internal Cleaning',
		price: '₱1,250',
		time: '2-3 hours',
		description: 'Thorough internal component cleaning'
	},
	{
		id: 'gpu',
		name: 'GPU Deep Cleaning',
		price: '₱600',
		time: '1.5-2 hours',
		description: 'Specialized GPU maintenance'
	},
	{
		id: 'psu',
		name: 'PSU Cleaning',
		price: '₱450',
		time: '30mins - 1 hour',
		description: 'Power supply cleaning'
	},
];

export default function Dashboard() {
	const navigate = useNavigate();
	const [searchParams] = useSearchParams();
	const [user, setUser] = useState<any>(null);
	const [loading, setLoading] = useState(true);
	const [bookings, setBookings] = useState<Booking[]>([]);
	const [selectedBooking, setSelectedBooking] = useState<Booking | null>(null);
	const [showBookingModal, setShowBookingModal] = useState(false);
	
	// Handle OAuth redirect data
	useEffect(() => {
		const oauth = searchParams.get('oauth');
		const email = searchParams.get('user');
		const role = searchParams.get('role');
		
		if (oauth === 'success' && email && role) {
			const userData = {
				email: decodeURIComponent(email),
				role: role,
				name: decodeURIComponent(email).split('@')[0]
			};
			localStorage.setItem('cleanit.user', JSON.stringify(userData));
			navigate('/dashboard', { replace: true });
			window.location.reload();
		}
	}, [searchParams, navigate]);
	
	// Fetch user profile on mount
	useEffect(() => {
		const fetchUserProfile = async () => {
			const raw = localStorage.getItem('cleanit.user');
			if (!raw) {
				navigate('/login');
				return;
			}
			
			const storedUser = JSON.parse(raw);
			try {
				// Fetch fresh user data including verification status
				const response = await api.get(`/v1/user/profile/${storedUser.email}`);
				const updatedUser = { ...storedUser, ...response.data };
				localStorage.setItem('cleanit.user', JSON.stringify(updatedUser));
				setUser(updatedUser);
				
				// Fetch user's bookings
				try {
					const bookingsRes = await api.get(`/v1/bookings/client/${updatedUser.id}`);
					setBookings(bookingsRes.data);
				} catch (err) {
					console.error('Failed to fetch bookings', err);
				}
			} catch (err) {
				setUser(storedUser);
			} finally {
				setLoading(false);
			}
		};
		
		fetchUserProfile();
	}, [navigate]);

	if (loading) {
		return (
			<div className="min-h-screen bg-slate-50 flex items-center justify-center">
				<div className="text-violet-600">Loading...</div>
			</div>
		);
	}

	if (!user) return null;

	const isVerified = user?.verified === true;

	const handleLogout = () => {
		localStorage.removeItem('cleanit.user');
		navigate('/login');
	};

	const handleBookService = (service: Service) => {
		if (!isVerified) {
			alert('Please wait for admin verification before booking services.');
			return;
		}
		navigate(`/booking/${service.id}`);
	};

	return (
		<div className="min-h-screen bg-white">
			<div className="mx-auto max-w-6xl px-4 py-10">
				<div className="flex flex-col gap-6 md:flex-row md:items-start md:justify-between">
					<div>
						<h1 className="text-3xl font-bold text-slate-900">Welcome Back!</h1>
						<div className="mt-1 text-sm text-slate-500">Manage your bookings and explore our services</div>
					</div>

					<div className="w-full max-w-sm rounded-xl border border-violet-300 bg-white p-4 shadow-md">
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
								<div className="text-xs text-slate-500">📞 {user?.contactNo || 'No contact info'}</div>
							</div>
							<div className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
								isVerified 
									? 'bg-emerald-100 text-emerald-700' 
									: 'bg-rose-100 text-rose-700'
							}`}>
								{isVerified ? 'Verified' : 'Unverified'}
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

				<div className="mt-10 space-y-8">
					{/* My Booking Section */}
					<section>
						<h2 className="text-xl font-bold text-slate-900">My Booking</h2>
						{bookings.filter(b => ['pending', 'confirmed', 'in_progress'].includes(b.status)).length === 0 ? (
							<div className="mt-3 rounded-2xl border border-slate-200 bg-white p-10 text-center">
								<div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-slate-100 text-2xl">📅</div>
								<div className="mt-3 text-sm font-semibold text-slate-700">No Active Bookings</div>
								<div className="mt-1 text-xs text-slate-500">Your active bookings will appear here.</div>
							</div>
						) : (
							<div className="mt-3 space-y-3">
								{bookings
									.filter(b => ['pending', 'confirmed', 'in_progress'].includes(b.status))
									.map(booking => (
										<div key={booking.id} className="rounded-xl border border-violet-200 bg-white p-4 shadow-sm">
											<div className="flex items-center justify-between">
												<div>
													<h3 className="text-sm font-semibold text-slate-900">{booking.serviceType}</h3>
													<div className="mt-1 flex items-center gap-3 text-xs text-slate-500">
														<span>📅 {booking.bookingDate}</span>
														<span>🕐 {booking.timeSlot}</span>
													</div>
												</div>
												<div className="flex items-center gap-2">
													<span className={`rounded-full px-2 py-1 text-[10px] font-medium ${
														booking.status === 'pending' ? 'bg-orange-100 text-orange-700' :
														booking.status === 'confirmed' ? 'bg-emerald-100 text-emerald-700' :
														'bg-blue-100 text-blue-700'
													}`}>
														{booking.status}
													</span>
													<button
														onClick={() => {
															setSelectedBooking(booking);
															setShowBookingModal(true);
														}}
														className="flex items-center gap-1 rounded-lg bg-violet-100 px-2 py-1 text-xs font-medium text-violet-700 hover:bg-violet-200 transition-colors"
													>
														<Eye className="h-3 w-3" />
														View
													</button>
												</div>
											</div>
											{booking.technicianName && (
												<div className="mt-2 text-xs text-slate-500">
													Technician: {booking.technicianName}
												</div>
											)}
										</div>
									))}
							</div>
						)}
					</section>

					{/* Browse Services Section */}
					<section>
						<div className="flex items-center justify-between gap-4">
							<h2 className="text-xl font-bold text-slate-900">Browse Services</h2>
							<button className="rounded-lg border border-violet-300 bg-white px-3 py-1.5 text-xs font-semibold text-violet-700 hover:bg-violet-50">
								View All Services →
							</button>
						</div>

						{!isVerified && (
							<div className="mt-3 rounded-lg bg-amber-50 border border-amber-200 p-3">
								<p className="text-sm text-amber-800">
									🔒 Services are locked. Please wait for admin verification to unlock booking.
								</p>
							</div>
						)}

						<div className="mt-4 grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-4">
							{services.map((service) => (
								<div 
									key={service.id} 
									className={`overflow-hidden rounded-xl border bg-white shadow-sm transition-all ${
										isVerified 
											? 'border-violet-300 cursor-pointer hover:shadow-lg hover:scale-105' 
											: 'border-slate-200 opacity-75 cursor-not-allowed'
									}`}
									onClick={() => isVerified && handleBookService(service)}
								>
									<div className="relative h-32 overflow-hidden bg-slate-100">
										<img 
											src={serviceImages[service.name] || '/images/default-service.jpg'} 
											alt={service.name}
											className="h-full w-full object-cover"
											onError={(e) => {
												(e.target as HTMLImageElement).src = '/images/default-service.jpg';
											}}
										/>
										{!isVerified && (
											<div className="absolute inset-0 flex items-center justify-center bg-black/40">
												<span className="text-2xl">🔒</span>
											</div>
										)}
									</div>
									<div className="p-4">
										<div className="text-sm font-bold text-slate-900">{service.name}</div>
										<div className="mt-1 text-xs text-slate-500">{service.description}</div>
										<div className="mt-3 flex items-center justify-between">
											<div className="text-xs text-slate-500">🕒 {service.time}</div>
											<div className="text-sm font-bold text-violet-700">{service.price}</div>
										</div>
										<button 
											className={`mt-3 w-full rounded-lg py-2 text-xs font-semibold transition-colors ${
												isVerified
													? 'bg-violet-700 text-white hover:bg-violet-800'
													: 'bg-slate-200 text-slate-400 cursor-not-allowed'
											}`}
											disabled={!isVerified}
										>
											{isVerified ? 'Book Now' : 'Locked'}
										</button>
									</div>
								</div>
							))}
						</div>
					</section>

					{/* Booking History Section */}
					<section>
						<h2 className="text-xl font-bold text-slate-900">Booking History</h2>
						{bookings.filter(b => ['completed', 'cancelled'].includes(b.status)).length === 0 ? (
							<div className="mt-3 rounded-2xl border border-slate-200 bg-white p-10 text-center">
								<div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-slate-100 text-2xl">📅</div>
								<div className="mt-3 text-sm font-semibold text-slate-700">No Booking History</div>
								<div className="mt-1 text-xs text-slate-500">Your completed bookings will appear here.</div>
							</div>
						) : (
							<div className="mt-3 space-y-3">
								{bookings
									.filter(b => ['completed', 'cancelled'].includes(b.status))
									.map(booking => (
										<div key={booking.id} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm opacity-75">
											<div className="flex items-center justify-between">
												<div>
													<h3 className="text-sm font-semibold text-slate-900">{booking.serviceType}</h3>
													<div className="mt-1 text-xs text-slate-500">
														📅 {booking.bookingDate} • {booking.timeSlot}
													</div>
												</div>
												<div className="flex items-center gap-2">
													<span className={`rounded-full px-2 py-1 text-[10px] font-medium ${
														booking.status === 'completed' ? 'bg-emerald-100 text-emerald-700' :
														'bg-rose-100 text-rose-700'
													}`}>
														{booking.status}
													</span>
													<button
														onClick={() => {
															setSelectedBooking(booking);
															setShowBookingModal(true);
														}}
														className="flex items-center gap-1 rounded-lg bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600 hover:bg-slate-200 transition-colors"
													>
														<Eye className="h-3 w-3" />
														View
													</button>
												</div>
											</div>
										</div>
									))}
							</div>
						)}
					</section>
				</div>
			</div>

			{/* Booking Details Modal */}
			{showBookingModal && selectedBooking && (
				<div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
					<div className="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-2xl bg-white p-6 shadow-xl">
						<div className="mb-4 flex items-center justify-between">
							<h3 className="text-lg font-bold text-slate-900">Booking Details</h3>
							<button
								onClick={() => setShowBookingModal(false)}
								className="rounded-full p-1 hover:bg-slate-100"
							>
								✕
							</button>
						</div>
						
						<div className="space-y-4">
							<div className="rounded-lg bg-violet-50 p-3">
								<div className="text-xs text-slate-500">Service</div>
								<div className="font-semibold text-slate-900">{selectedBooking.serviceType}</div>
								<div className="mt-1 text-xs text-violet-600">₱{selectedBooking.totalAmount}</div>
							</div>
							
							<div className="grid grid-cols-2 gap-3 text-sm">
								<div>
									<div className="text-xs text-slate-500">Device</div>
									<div className="font-medium text-slate-900 capitalize">{selectedBooking.deviceType}</div>
								</div>
								<div>
									<div className="text-xs text-slate-500">Status</div>
									<span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
										selectedBooking.status === 'pending' ? 'bg-orange-100 text-orange-700' :
										selectedBooking.status === 'confirmed' ? 'bg-emerald-100 text-emerald-700' :
										selectedBooking.status === 'in_progress' ? 'bg-blue-100 text-blue-700' :
										selectedBooking.status === 'completed' ? 'bg-emerald-100 text-emerald-700' :
										'bg-rose-100 text-rose-700'
									}`}>
										{selectedBooking.status}
									</span>
								</div>
							</div>
							
							<div className="grid grid-cols-2 gap-3 text-sm">
								<div>
									<div className="text-xs text-slate-500">Date</div>
									<div className="font-medium text-slate-900">{selectedBooking.bookingDate}</div>
								</div>
								<div>
									<div className="text-xs text-slate-500">Time</div>
									<div className="font-medium text-slate-900">{selectedBooking.timeSlot}</div>
								</div>
							</div>
							
							{selectedBooking.technicianName && (
								<div className="rounded-lg bg-slate-50 p-3">
									<div className="text-xs text-slate-500">Technician</div>
									<div className="font-semibold text-slate-900">{selectedBooking.technicianName}</div>
									{selectedBooking.technicianContact && (
										<div className="text-xs text-slate-500">{selectedBooking.technicianContact}</div>
									)}
								</div>
							)}
							
							<div className="rounded-lg bg-slate-50 p-3">
								<div className="text-xs text-slate-500">Address</div>
								<div className="font-medium text-slate-900">{selectedBooking.address}</div>
								{selectedBooking.landmark && (
									<div className="text-xs text-slate-500">Landmark: {selectedBooking.landmark}</div>
								)}
							</div>
							
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
