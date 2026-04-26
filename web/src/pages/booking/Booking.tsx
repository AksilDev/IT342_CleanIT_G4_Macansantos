import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Check, AlertCircle } from 'lucide-react';
import api from '../../api/axios';

// Service images mapping (for UI display only)
const serviceImages: Record<string, string> = {
	'Standard External Cleaning': '/images/external-cleaning.jpg',
	'Deep Internal Cleaning': '/images/internal-cleaning.jpg',
	'GPU Deep Cleaning': '/images/gpu-cleaning.jpg',
	'PSU Cleaning': '/images/psu-cleaning.jpg',
};

const timeSlots: TimeSlot[] = [
	{ id: '9-11', label: '9:00 AM - 11:00 AM' },
	{ id: '11-1', label: '11:00 AM - 1:00 PM' },
	{ id: '1-3', label: '1:00 PM - 3:00 PM' },
	{ id: '3-5', label: '3:00 PM - 5:00 PM' },
];

// Types
interface ServiceInfo {
	id: string;
	name: string;
	description: string;
	durationMinutes: number;
	basePrice: number;
	isActive: boolean;
}

interface AddOnService {
	id: string;
	name: string;
	description: string;
	price: number;
	isActive: boolean;
}

interface Technician {
	id: string;
	name: string;
	email: string;
	contactNo?: string;
	imageUrl?: string;
	available: boolean;
}

interface TimeSlot {
	id: string;
	label: string;
}

export default function Booking() {
	const navigate = useNavigate();
	const { serviceId } = useParams<{ serviceId: string }>();

	const [service, setService] = useState<ServiceInfo | null>(null);
	const [allServices, setAllServices] = useState<ServiceInfo[]>([]);
	const [compatibleAddOns, setCompatibleAddOns] = useState<AddOnService[]>([]);
	const [technicians, setTechnicians] = useState<Technician[]>([]);
	const [loadingTechs, setLoadingTechs] = useState(true);
	const [selectedDevice, setSelectedDevice] = useState<'pc' | 'laptop' | null>(null);
	const [selectedAddOns, setSelectedAddOns] = useState<string[]>([]);
	const [selectedTechnician, setSelectedTechnician] = useState<string | null>(null);
	const [selectedTimeSlot, setSelectedTimeSlot] = useState<string | null>(null);
	const [address, setAddress] = useState('');
	const [landmark, setLandmark] = useState('');
	const [specialInstructions, setSpecialInstructions] = useState('');
	const [bookingDate, setBookingDate] = useState<string>(new Date().toISOString().split('T')[0]);
	const [submitting, setSubmitting] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const user = React.useMemo(() => {
		try {
			const raw = localStorage.getItem('cleanit.user');
			return raw ? JSON.parse(raw) : null;
		} catch {
			return null;
		}
	}, []);

	useEffect(() => {
		if (!user) {
			navigate('/login');
			return;
		}
		
		// Fetch services from API
		const fetchServices = async () => {
			try {
				const response = await api.get('/v1/services');
				setAllServices(response.data);
				
				// If serviceId is provided, find and set the service
				if (serviceId) {
					const found = response.data.find((s: ServiceInfo) => s.id === serviceId);
					if (found) {
						setService(found);
						// Fetch compatible add-ons for this service
						fetchCompatibleAddOns(found.id);
					} else {
						navigate('/dashboard');
					}
				}
			} catch (err) {
				console.error('Failed to fetch services', err);
				navigate('/dashboard');
			}
		};
		
		fetchServices();
	}, [serviceId, navigate, user]);

	const fetchCompatibleAddOns = async (serviceId: string) => {
		try {
			const response = await api.get(`/v1/services/${serviceId}/addons`);
			setCompatibleAddOns(response.data);
		} catch (err) {
			console.error('Failed to fetch compatible add-ons', err);
			setCompatibleAddOns([]);
		}
	};

	useEffect(() => {
		const fetchTechnicians = async () => {
			try {
				const response = await api.get('/v1/user/technicians/verified');
				setTechnicians(response.data);
			} catch (err) {
				console.error('Failed to fetch technicians', err);
				setTechnicians([]);
			} finally {
				setLoadingTechs(false);
			}
		};
		fetchTechnicians();
	}, []);

	const toggleAddOn = (addOnId: string) => {
		setSelectedAddOns((prev) =>
			prev.includes(addOnId) ? prev.filter((id) => id !== addOnId) : [...prev, addOnId]
		);
	};

	const calculateTotal = () => {
		if (!service) return 0;
		const addOnTotal = selectedAddOns.reduce((sum, addOnId) => {
			const addOn = compatibleAddOns.find((a: AddOnService) => a.id === addOnId);
			return sum + (addOn?.price || 0);
		}, 0);
		return service.basePrice + addOnTotal;
	};

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		if (!selectedDevice || !selectedTechnician || !selectedTimeSlot || !address) {
			alert('Please fill in all required fields');
			return;
		}
		if (!user?.id) {
			alert('Please login again');
			return;
		}
		if (!service) {
			alert('Please select a service');
			return;
		}

		setSubmitting(true);
		setError(null);
		try {
			const bookingData = {
				clientId: user.id,
				serviceId: service.id, // Send service ID instead of name
				serviceType: service.name, // Keep for backward compatibility
				deviceType: selectedDevice,
				addOns: selectedAddOns, // Send addon IDs
				timeSlot: selectedTimeSlot,
				bookingDate: bookingDate,
				address: address,
				landmark: landmark,
				specialInstructions: specialInstructions,
				totalAmount: calculateTotal(),
			};

			await api.post('/v1/bookings/create', bookingData);
			alert('Booking submitted successfully!');
			navigate('/dashboard?refresh=true');
		} catch (err: any) {
			const errorMessage = err?.response?.data?.message || 'Failed to submit booking';
			setError(errorMessage);
			alert(errorMessage);
		} finally {
			setSubmitting(false);
		}
	};

	if (!service) {
		return (
			<div className="min-h-screen bg-slate-50 flex items-center justify-center">
				<div className="text-violet-600">Loading...</div>
			</div>
		);
	}

	return (
		<div className="min-h-screen bg-white">
			<div className="mx-auto max-w-2xl px-4 py-6">
				{/* Header */}
				<div className="mb-6 flex items-start justify-between">
					<button
						onClick={() => navigate('/dashboard')}
						className="flex items-center gap-2 rounded-lg bg-violet-500 px-4 py-2 text-sm font-medium text-white hover:bg-violet-600 transition-colors"
					>
						<ArrowLeft className="h-4 w-4" />
						Back to home
					</button>
					<div className="text-center flex-1 ml-4">
						<h1 className="text-xl font-bold text-slate-900">Book Your Service</h1>
						<p className="text-xs text-slate-500 mt-1">Complete the details below to schedule your cleaning</p>
					</div>
					<div className="w-28" /> {/* Spacer for balance */}
				</div>

				{/* Progress Bar */}
				<div className="mb-6 h-2 w-full rounded-full bg-slate-200">
					<div className="h-full w-3/5 rounded-full bg-violet-500" />
				</div>

				{/* Error Alert */}
				{error && (
					<div className="mb-6 rounded-lg border border-rose-200 bg-rose-50 p-4 flex items-start gap-3">
						<AlertCircle className="h-5 w-5 text-rose-600 flex-shrink-0 mt-0.5" />
						<div>
							<p className="text-sm font-medium text-rose-800">Booking Error</p>
							<p className="text-sm text-rose-700 mt-1">{error}</p>
						</div>
					</div>
				)}

				<form onSubmit={handleSubmit} className="space-y-6">
					{/* Selected Service */}
					<div className="rounded-xl border border-violet-200 bg-white p-4 shadow-sm">
						<div className="text-xs font-semibold text-slate-700 mb-3">Selected Service</div>
						<div className="flex items-start justify-between">
							<div className="flex gap-3">
								<div className="h-16 w-16 rounded-lg bg-slate-100 overflow-hidden">
									<img
										src={serviceImages[service.name] || '/images/default-service.jpg'}
										alt={service.name}
										className="h-full w-full object-cover"
										onError={(e) => {
											(e.target as HTMLImageElement).src = '/images/default-service.jpg';
										}}
									/>
								</div>
								<div>
									<div className="text-xs font-semibold text-slate-900">{service.name}</div>
									<div className="text-[10px] text-slate-400 mt-0.5">{service.description}</div>
									<div className="text-[10px] text-slate-400">Duration: {service.durationMinutes} mins</div>
								</div>
							</div>
							<div className="text-sm font-bold text-violet-600">₱{service.basePrice}</div>
						</div>
					</div>

					{/* Select Device */}
					<div>
						<div className="text-xs font-semibold text-slate-700 mb-3">Select Device</div>
						<div className="grid grid-cols-2 gap-3">
							<button
								type="button"
								onClick={() => setSelectedDevice('pc')}
								className={`flex items-center gap-3 rounded-lg border p-3 transition-all ${
									selectedDevice === 'pc'
										? 'border-violet-400 bg-violet-50'
										: 'border-slate-200 bg-white hover:border-violet-300'
								}`}
							>
								<div className={`flex h-4 w-4 items-center justify-center rounded border ${
									selectedDevice === 'pc' ? 'border-violet-500 bg-violet-500' : 'border-slate-300'
								}`}>
									{selectedDevice === 'pc' && <Check className="h-3 w-3 text-white" />}
								</div>
								<span className="text-xs font-medium text-slate-700">PC</span>
							</button>
							<button
								type="button"
								onClick={() => setSelectedDevice('laptop')}
								className={`flex items-center gap-3 rounded-lg border p-3 transition-all ${
									selectedDevice === 'laptop'
										? 'border-violet-400 bg-violet-50'
										: 'border-slate-200 bg-white hover:border-violet-300'
								}`}
							>
								<div className={`flex h-4 w-4 items-center justify-center rounded border ${
									selectedDevice === 'laptop' ? 'border-violet-500 bg-violet-500' : 'border-slate-300'
								}`}>
									{selectedDevice === 'laptop' && <Check className="h-3 w-3 text-white" />}
								</div>
								<span className="text-xs font-medium text-slate-700">Laptop</span>
							</button>
						</div>
					</div>

					{/* Add-on Services */}
					<div>
						<div className="text-xs font-semibold text-slate-700 mb-3">Add-on Services</div>
						{compatibleAddOns.length === 0 ? (
							<div className="text-xs text-slate-500 text-center py-4">Loading add-ons...</div>
						) : (
							<div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
								{compatibleAddOns.map((addOn: AddOnService) => (
								<button
									key={addOn.id}
									type="button"
									onClick={() => toggleAddOn(addOn.id)}
									className={`flex items-center justify-between rounded-lg border p-3 transition-all ${
										selectedAddOns.includes(addOn.id)
											? 'border-violet-400 bg-violet-50'
											: 'border-slate-200 bg-white hover:border-violet-300'
									}`}
								>
									<div className="flex items-center gap-3">
										<div className={`flex h-4 w-4 items-center justify-center rounded border ${
											selectedAddOns.includes(addOn.id) ? 'border-violet-500 bg-violet-500' : 'border-slate-300'
										}`}>
											{selectedAddOns.includes(addOn.id) && <Check className="h-3 w-3 text-white" />}
										</div>
										<span className="text-xs font-medium text-slate-700">{addOn.name}</span>
									</div>
										<span className="text-xs font-bold text-violet-600">₱{addOn.price}</span>
									</button>
								))}
							</div>
						)}
					</div>

					{/* Select Technician */}
					<div>
						<div className="text-xs font-semibold text-slate-700 mb-3">Select Technician</div>
						{loadingTechs ? (
							<div className="text-xs text-slate-500 text-center py-4">Loading technicians...</div>
						) : technicians.length === 0 ? (
							<div className="text-xs text-slate-500 text-center py-4">No verified technicians available</div>
						) : (
							<div className="space-y-3">
								{technicians.map((tech) => (
									<button
										key={tech.id}
										type="button"
										onClick={() => setSelectedTechnician(tech.id)}
										className={`w-full flex items-center justify-between rounded-lg border p-4 transition-all ${
											selectedTechnician === tech.id
												? 'border-violet-400 bg-violet-50'
												: 'border-slate-200 bg-white hover:border-violet-300'
										}`}
									>
										<div className="flex items-center gap-3">
											<div className={`h-4 w-4 rounded-full border ${
												selectedTechnician === tech.id ? 'border-violet-500 bg-violet-500' : 'border-slate-300'
											}`} />
											<div className="text-left">
												<div className="text-xs font-semibold text-slate-900">{tech.name}</div>
												<div className="text-[10px] text-slate-500">{tech.contactNo || 'No contact info'}</div>
											</div>
										</div>
										{tech.available && (
											<span className="text-[10px] text-emerald-600 bg-emerald-100 px-2 py-0.5 rounded-full">
												Available
											</span>
										)}
									</button>
								))}
							</div>
						)}
					</div>

					{/* Available Time Slots */}
					<div>
						<div className="text-xs font-semibold text-slate-700 mb-3">Available Time Slots</div>
						<div className="grid grid-cols-2 gap-3">
							{timeSlots.map((slot) => (
								<button
									key={slot.id}
									type="button"
									onClick={() => setSelectedTimeSlot(slot.id)}
									className={`flex items-center gap-2 rounded-lg border p-3 transition-all ${
										selectedTimeSlot === slot.id
											? 'border-violet-400 bg-violet-50'
											: 'border-slate-200 bg-white hover:border-violet-300'
									}`}
								>
									<div className={`h-4 w-4 rounded-full border ${
										selectedTimeSlot === slot.id ? 'border-violet-500 bg-violet-500' : 'border-slate-300'
									}`} />
									<span className="text-xs font-medium text-slate-700">{slot.label}</span>
								</button>
							))}
						</div>
					</div>

					{/* Service Location */}
					<div className="space-y-4">
						<div className="text-xs font-semibold text-slate-700">Service Location</div>
						
						<div>
							<label className="text-xs font-medium text-slate-600">Address *</label>
							<input
								type="text"
								value={address}
								onChange={(e) => setAddress(e.target.value)}
								placeholder="Enter your complete address"
								className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2.5 text-xs text-slate-700 outline-none focus:border-violet-400 focus:ring-2 focus:ring-violet-100"
								required
							/>
						</div>

						<div>
							<label className="text-xs font-medium text-slate-600">Landmark</label>
							<input
								type="text"
								value={landmark}
								onChange={(e) => setLandmark(e.target.value)}
								placeholder="Nearby landmark for easy location"
								className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2.5 text-xs text-slate-700 outline-none focus:border-violet-400 focus:ring-2 focus:ring-violet-100"
							/>
						</div>

						<div>
							<label className="text-xs font-medium text-slate-600">Special Instructions</label>
							<input
								type="text"
								value={specialInstructions}
								onChange={(e) => setSpecialInstructions(e.target.value)}
								placeholder="Ex. contact #"
								className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2.5 text-xs text-slate-700 outline-none focus:border-violet-400 focus:ring-2 focus:ring-violet-100"
							/>
						</div>
					</div>

					{/* Price Summary */}
					<div className="rounded-lg border border-violet-200 bg-violet-50 p-4">
						<div className="flex items-center justify-between mb-2">
							<span className="text-xs text-slate-600">Base Service</span>
							<span className="text-xs font-semibold text-slate-700">₱{service.basePrice}</span>
						</div>
							{selectedAddOns.length > 0 && (
								<div className="space-y-1 mb-2">
									{selectedAddOns.map((addOnId) => {
										const addOn = compatibleAddOns.find((a: AddOnService) => a.id === addOnId);
										return addOn ? (
											<div key={addOnId} className="flex items-center justify-between">
												<span className="text-[10px] text-slate-500">+ {addOn.name}</span>
												<span className="text-[10px] text-slate-500">₱{addOn.price}</span>
											</div>
											) : null;
										})}
									</div>
								)}
						<div className="border-t border-violet-200 pt-2 flex items-center justify-between">
							<span className="text-xs font-semibold text-slate-700">Total Amount</span>
							<span className="text-base font-bold text-violet-600">₱{calculateTotal()}</span>
						</div>
					</div>

					{/* Submit Button */}
					<button
						type="submit"
						disabled={submitting}
						className="w-full flex items-center justify-center gap-2 rounded-lg bg-violet-600 px-4 py-3 text-sm font-semibold text-white hover:bg-violet-700 transition-colors shadow-lg shadow-violet-200 disabled:opacity-60"
					>
						{submitting ? 'Submitting...' : 'Submit Booking'}
						<span>→</span>
					</button>
				</form>
			</div>
		</div>
	);
}
