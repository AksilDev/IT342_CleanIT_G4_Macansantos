import React, { useState, useEffect } from 'react';
import api from '../api/axios';

interface Booking {
	id: string;
	bookingCode: string;
	clientName: string;
	clientEmail: string;
	clientContact: string;
	technicianName?: string;
	technicianEmail?: string;
	technicianContact?: string;
	serviceType: string;
	deviceType: string;
	addOns: string[];
	totalAmount: number;
	bookingDate: string;
	timeSlot: string;
	address: string;
	landmark?: string;
	specialInstructions?: string;
	status: string;
	statusDescription: string;
	createdAt: string;
	confirmedAt?: string;
	startedAt?: string;
	completedAt?: string;
	cancelledAt?: string;
}

interface BookingDetailsModalProps {
	isOpen: boolean;
	onClose: () => void;
	title: string;
	statuses: string[];
	initialCount: number;
}

type SortField = 'bookingDate' | 'totalAmount' | 'createdAt';
type SortDirection = 'asc' | 'desc';

export default function BookingDetailsModal({
	isOpen,
	onClose,
	title,
	statuses,
	initialCount
}: BookingDetailsModalProps) {
	const [bookings, setBookings] = useState<Booking[]>([]);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [searchQuery, setSearchQuery] = useState('');
	const [sortField, setSortField] = useState<SortField>('createdAt');
	const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
	const [voidingBookingId, setVoidingBookingId] = useState<string | null>(null);

	// Fetch bookings when modal opens
	useEffect(() => {
		if (isOpen) {
			fetchBookings();
		}
	}, [isOpen, statuses]);

	const fetchBookings = async () => {
		try {
			setLoading(true);
			setError(null);
			
			const statusesParam = statuses.join(',');
			const response = await api.get(`/v1/admin/bookings/by-status?statuses=${statusesParam}`);
			
			setBookings(response.data.bookings);
		} catch (err: any) {
			console.error('Failed to fetch bookings:', err);
			setError(err.response?.data?.message || 'Failed to load bookings. Please try again.');
		} finally {
			setLoading(false);
		}
	};

	const handleRetry = () => {
		fetchBookings();
	};

	const handleVoidBooking = async (bookingId: string, bookingCode: string) => {
		if (!confirm(`Are you sure you want to VOID booking ${bookingCode}? This will cancel the booking and notify both the client and technician that it was terminated by an administrator.`)) {
			return;
		}

		try {
			setVoidingBookingId(bookingId);
			await api.post(`/v1/admin/bookings/${bookingId}/void`);
			
			// Remove the voided booking from the list
			setBookings(prev => prev.filter(b => b.id !== bookingId));
			
			alert(`Booking ${bookingCode} has been successfully voided.`);
		} catch (err: any) {
			console.error('Failed to void booking:', err);
			alert(err.response?.data?.message || 'Failed to void booking. Please try again.');
		} finally {
			setVoidingBookingId(null);
		}
	};

	const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
		if (e.target === e.currentTarget) {
			onClose();
		}
	};

	const handleKeyDown = (e: React.KeyboardEvent) => {
		if (e.key === 'Escape') {
			onClose();
		}
	};

	// Filter bookings by search query
	const filteredBookings = bookings.filter(booking => {
		if (!searchQuery.trim()) return true;
		
		const lowerQuery = searchQuery.toLowerCase();
		return (
			booking.clientName.toLowerCase().includes(lowerQuery) ||
			booking.clientEmail.toLowerCase().includes(lowerQuery) ||
			booking.bookingCode.toLowerCase().includes(lowerQuery) ||
			(booking.technicianName?.toLowerCase().includes(lowerQuery) ?? false) ||
			(booking.technicianEmail?.toLowerCase().includes(lowerQuery) ?? false)
		);
	});

	// Sort bookings
	const sortedBookings = [...filteredBookings].sort((a, b) => {
		let aValue: any;
		let bValue: any;
		
		switch (sortField) {
			case 'bookingDate':
				aValue = new Date(a.bookingDate).getTime();
				bValue = new Date(b.bookingDate).getTime();
				break;
			case 'totalAmount':
				aValue = a.totalAmount;
				bValue = b.totalAmount;
				break;
			case 'createdAt':
				aValue = new Date(a.createdAt).getTime();
				bValue = new Date(b.createdAt).getTime();
				break;
		}
		
		if (sortDirection === 'asc') {
			return aValue - bValue;
		} else {
			return bValue - aValue;
		}
	});

	if (!isOpen) return null;

	return (
		<div
			className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
			onClick={handleBackdropClick}
			onKeyDown={handleKeyDown}
			role="dialog"
			aria-modal="true"
			aria-labelledby="modal-title"
		>
			<div className="max-h-[90vh] w-full max-w-6xl overflow-y-auto rounded-2xl bg-white shadow-2xl">
				{/* Modal Header */}
				<div className="sticky top-0 z-10 border-b border-slate-200 bg-white px-6 py-4">
					<div className="flex items-center justify-between mb-4">
						<h2 id="modal-title" className="text-2xl font-bold text-slate-900">
							{title} ({sortedBookings.length})
						</h2>
						<button
							onClick={onClose}
							className="rounded-full p-2 hover:bg-slate-100 text-slate-500 transition-colors"
							aria-label="Close modal"
						>
							✕
						</button>
					</div>

					{/* Search and Sort Controls */}
					<div className="flex flex-col md:flex-row gap-3">
						{/* Search Input */}
						<div className="flex-1">
							<input
								type="text"
								placeholder="🔍 Search bookings..."
								value={searchQuery}
								onChange={(e) => setSearchQuery(e.target.value)}
								className="w-full rounded-lg border border-slate-300 px-4 py-2 text-sm focus:border-violet-500 focus:outline-none focus:ring-2 focus:ring-violet-200"
							/>
						</div>

						{/* Sort Controls */}
						<div className="flex gap-2">
							<select
								value={sortField}
								onChange={(e) => setSortField(e.target.value as SortField)}
								className="rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-violet-500 focus:outline-none focus:ring-2 focus:ring-violet-200"
							>
								<option value="createdAt">Creation Date</option>
								<option value="bookingDate">Booking Date</option>
								<option value="totalAmount">Total Amount</option>
							</select>

							<button
								onClick={() => setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc')}
								className="rounded-lg border border-slate-300 px-3 py-2 text-sm hover:bg-slate-50 transition-colors"
								aria-label={`Sort ${sortDirection === 'asc' ? 'descending' : 'ascending'}`}
							>
								{sortDirection === 'asc' ? '↑' : '↓'}
							</button>
						</div>
					</div>

					{/* Results Count */}
					{searchQuery && (
						<p className="mt-2 text-sm text-slate-600">
							Showing {sortedBookings.length} of {bookings.length} bookings
						</p>
					)}
				</div>

				{/* Modal Content */}
				<div className="p-6">
					{loading ? (
						<div className="flex flex-col items-center justify-center py-12">
							<div className="h-12 w-12 animate-spin rounded-full border-4 border-violet-200 border-t-violet-600"></div>
							<p className="mt-4 text-sm text-slate-600">Loading bookings...</p>
						</div>
					) : error ? (
						<div className="rounded-lg border border-rose-200 bg-rose-50 p-4">
							<div className="flex items-center gap-2 text-rose-700">
								<span className="text-xl">⚠️</span>
								<span className="font-medium">{error}</span>
							</div>
							<button
								onClick={handleRetry}
								className="mt-3 rounded-lg bg-rose-600 px-4 py-2 text-sm text-white hover:bg-rose-700 transition-colors"
							>
								Retry
							</button>
						</div>
					) : sortedBookings.length === 0 ? (
						<div className="flex flex-col items-center justify-center py-12">
							<div className="text-6xl mb-4">📋</div>
							<p className="text-lg font-medium text-slate-700">No bookings found</p>
							<p className="text-sm text-slate-500 mt-1">
								{searchQuery ? 'Try adjusting your search query' : 'There are no bookings in this category yet.'}
							</p>
						</div>
					) : (
						<div className="space-y-3">
							{sortedBookings.map((booking) => (
								<div
									key={booking.id}
									className="rounded-xl border border-slate-200 bg-white p-4 hover:shadow-md transition-shadow"
								>
									{/* Booking Header */}
									<div className="flex items-start justify-between mb-3">
										<div>
											<h3 className="text-lg font-semibold text-slate-900">{booking.bookingCode}</h3>
											<p className="text-xs text-slate-500">
												Created: {new Date(booking.createdAt).toLocaleString()}
											</p>
										</div>
										<span
											className={`inline-flex px-3 py-1 rounded-full text-xs font-medium ${
												booking.status === 'pending'
													? 'bg-orange-100 text-orange-700'
													: booking.status === 'confirmed'
													? 'bg-green-100 text-green-700'
													: booking.status === 'in_progress'
													? 'bg-blue-100 text-blue-700'
													: booking.status === 'completed'
													? 'bg-gray-100 text-gray-700'
													: 'bg-red-100 text-red-700'
											}`}
										>
											{booking.status.toUpperCase().replace('_', ' ')}
										</span>
									</div>

									{/* Booking Details Grid */}
									<div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
										{/* Client Info */}
										<div>
											<p className="font-medium text-slate-700 mb-1">Client</p>
											<p className="text-slate-900">{booking.clientName}</p>
											<p className="text-slate-600">{booking.clientEmail}</p>
											<p className="text-slate-600">{booking.clientContact}</p>
										</div>

										{/* Technician Info */}
										<div>
											<p className="font-medium text-slate-700 mb-1">Technician</p>
											{booking.technicianName ? (
												<>
													<p className="text-slate-900">{booking.technicianName}</p>
													<p className="text-slate-600">{booking.technicianEmail}</p>
													<p className="text-slate-600">{booking.technicianContact}</p>
												</>
											) : (
												<p className="text-slate-500 italic">Not assigned yet</p>
											)}
										</div>

										{/* Service Details */}
										<div>
											<p className="font-medium text-slate-700 mb-1">Service</p>
											<p className="text-slate-900">{booking.serviceType}</p>
											<p className="text-slate-600">Device: {booking.deviceType}</p>
											{booking.addOns.length > 0 && (
												<p className="text-slate-600">Add-ons: {booking.addOns.join(', ')}</p>
											)}
										</div>

										{/* Booking Details */}
										<div>
											<p className="font-medium text-slate-700 mb-1">Schedule & Price</p>
											<p className="text-slate-900">
												{new Date(booking.bookingDate).toLocaleDateString('en-US', {
													year: 'numeric',
													month: 'short',
													day: 'numeric'
												})}
											</p>
											<p className="text-slate-600">{booking.timeSlot}</p>
											<p className="text-lg font-bold text-violet-600">₱{booking.totalAmount.toFixed(2)}</p>
										</div>

										{/* Location */}
										<div className="md:col-span-2">
											<p className="font-medium text-slate-700 mb-1">Location</p>
											<p className="text-slate-900">{booking.address}</p>
											{booking.landmark && <p className="text-slate-600">Landmark: {booking.landmark}</p>}
										</div>

										{/* Special Instructions */}
										{booking.specialInstructions && (
											<div className="md:col-span-2">
												<p className="font-medium text-slate-700 mb-1">Special Instructions</p>
												<p className="text-slate-600">{booking.specialInstructions}</p>
											</div>
										)}
									</div>

									{/* Admin Actions */}
									{(booking.status === 'pending' || booking.status === 'confirmed' || booking.status === 'in_progress') && (
										<div className="mt-4 pt-4 border-t border-slate-200">
											<button
												onClick={() => handleVoidBooking(booking.id, booking.bookingCode)}
												disabled={voidingBookingId === booking.id}
												className="w-full rounded-lg bg-rose-600 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
											>
												{voidingBookingId === booking.id ? 'Voiding...' : '🚫 Void/Terminate Booking'}
											</button>
											<p className="mt-2 text-xs text-slate-500 text-center">
												This will cancel the booking and notify both client and technician
											</p>
										</div>
									)}
								</div>
							))}
						</div>
					)}
				</div>
			</div>
		</div>
	);
}
