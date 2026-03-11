import React, { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../api/axios';

type RegisterResponse = {
	message?: string;
};

export default function Register() {
	const navigate = useNavigate();
	const [name, setName] = useState('');
	const [username, setUsername] = useState('');
	const [password, setPassword] = useState('');
	const [contact, setContact] = useState('');
	const [role, setRole] = useState('client');
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const canSubmit = useMemo(() => {
		return name.trim().length > 0 && username.trim().length > 0 && password.trim().length > 0;
	}, [name, username, password]);

	async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
		e.preventDefault();
		setError(null);
		setLoading(true);

		try {
			await api.post<RegisterResponse>('/v1/auth/register', { name, email: username, password, contactNo: contact, role: role.toLowerCase() });
			navigate('/login');
		} catch (err: any) {
			setError(err?.response?.data?.message ?? 'Registration failed');
		} finally {
			setLoading(false);
		}
	}

	return (
		<div className="min-h-screen w-full bg-white">
			<div className="min-h-screen w-full bg-[radial-gradient(circle_at_15%_10%,rgba(124,58,237,0.18),transparent_55%),radial-gradient(circle_at_80%_40%,rgba(99,102,241,0.14),transparent_55%),linear-gradient(180deg,rgba(255,255,255,1),rgba(248,250,252,1))]">
				<div className="mx-auto max-w-6xl px-4 py-10">
					<div className="text-center">
						<h1 className="text-4xl font-extrabold text-violet-700">Create Your CleanIT Account</h1>
						<div className="mt-2 text-sm text-slate-600">
							Join CleanIT{' '}
							<span className="mx-2 text-slate-300">|</span>
							Already have an account?{' '}
							<Link to="/login" className="font-semibold text-violet-700 hover:text-violet-800">
								Sign in here
							</Link>
						</div>
						<div className="mx-auto mt-4 h-1 w-72 rounded-full bg-violet-200">
							<div className="h-1 w-12 rounded-full bg-violet-600" />
						</div>
					</div>

					<div className="mx-auto mt-8 w-full max-w-4xl rounded-2xl bg-white p-8 shadow-xl">
						<div className="text-sm font-semibold text-slate-700">Registration Progress</div>

						<form className="mt-6 space-y-5" onSubmit={onSubmit}>
							<div>
								<label className="text-sm font-medium text-slate-700">Name</label>
								<input
									value={name}
									onChange={(e) => setName(e.target.value)}
									placeholder="Enter your name"
									className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-100"
								/>
							</div>

							<div className="grid grid-cols-1 gap-5 md:grid-cols-2">
								<div>
									<label className="text-sm font-medium text-slate-700">Email</label>
									<input
										type="email"
										value={username}
										onChange={(e) => setUsername(e.target.value)}
										placeholder="Enter your email"
										className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-100"
										required
									/>
								</div>
								<div>
									<label className="text-sm font-medium text-slate-700">Password</label>
									<input
										type="password"
										value={password}
										onChange={(e) => setPassword(e.target.value)}
										placeholder="Enter your password"
										className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-100"
										required
									/>
								</div>
							</div>

							<div className="grid grid-cols-1 gap-5 md:grid-cols-2">
								<div>
									<label className="text-sm font-medium text-slate-700">Contact No.#</label>
									<input
										value={contact}
										onChange={(e) => setContact(e.target.value)}
										placeholder="+63 0000 000 000"
										className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-100"
									/>
								</div>
								<div>
									<label className="text-sm font-medium text-slate-700">Role</label>
									<select
										value={role}
										onChange={(e) => setRole(e.target.value)}
										className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-100"
									>
										<option>client</option>
										<option>technician</option>
									</select>
								</div>
							</div>

							
							{error ? (
								<div className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>
							) : null}

							<div className="flex justify-center pt-2">
								<button
									type="submit"
									disabled={!canSubmit || loading}
									className="rounded-xl bg-violet-700 px-10 py-3 text-sm font-semibold text-white shadow-lg shadow-violet-200 transition hover:bg-violet-800 disabled:opacity-60"
								>
									{loading ? 'Submitting...' : 'Complete Form →'}
								</button>
							</div>

							<div className="pt-4 text-center text-xs text-slate-500">
								By signing up, you agree to our{' '}
								<span className="font-semibold text-violet-700">Terms and Conditions</span>
							</div>
						</form>
					</div>
				</div>
			</div>
		</div>
	);
}
