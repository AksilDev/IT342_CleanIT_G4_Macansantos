import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../api/axios';

type LoginResponse = {
	message?: string;
	username?: string;
};

export default function Login() {
	const navigate = useNavigate();
	const [username, setUsername] = useState('');
	const [password, setPassword] = useState('');
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);

	async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
		e.preventDefault();
		setError(null);
		setLoading(true);

		try {
			const res = await api.post<LoginResponse>('/auth/login', { username, password });
			localStorage.setItem('cleanit.user', JSON.stringify(res.data ?? {}));
			navigate('/dashboard');
		} catch (err: any) {
			setError(err?.response?.data?.message ?? 'Login failed');
		} finally {
			setLoading(false);
		}
	}

	return (
		<div className="min-h-screen w-full bg-neutral-900">
			<div className="min-h-screen w-full bg-[radial-gradient(circle_at_20%_10%,rgba(124,58,237,0.35),transparent_45%),radial-gradient(circle_at_80%_80%,rgba(99,102,241,0.25),transparent_45%),linear-gradient(135deg,rgba(15,23,42,0.95),rgba(3,7,18,0.95))]">
				<div className="mx-auto flex min-h-screen max-w-6xl flex-col items-center justify-center px-4">
					<div className="mb-8 text-center">
						<div className="text-sm font-semibold tracking-widest text-white/70">Client LOGIN</div>
						<h1 className="mt-2 text-2xl font-semibold text-white">Extend the life of Your Gear.</h1>
					</div>

					<div className="w-full max-w-md rounded-3xl bg-white p-8 shadow-2xl">
						<div className="text-center">
							<div className="text-4xl font-extrabold tracking-wide text-[#2b0a3d]">WELCOME</div>
							<div className="mt-1 text-sm text-slate-500">Sign in to your account to continue</div>
						</div>

						<form className="mt-8 space-y-5" onSubmit={onSubmit}>
							<div>
								<label className="text-sm font-medium text-slate-700">Username</label>
								<input
									value={username}
									onChange={(e) => setUsername(e.target.value)}
									placeholder="Enter your username"
									className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 outline-none transition focus:border-violet-500 focus:ring-4 focus:ring-violet-100"
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
									className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 outline-none transition focus:border-violet-500 focus:ring-4 focus:ring-violet-100"
									required
								/>
							</div>

							<div className="flex items-center justify-between text-xs text-slate-500">
								<label className="flex items-center gap-2">
									<input type="checkbox" className="h-3.5 w-3.5 rounded border-slate-300" />
									<span>Remember me</span>
								</label>
								<button type="button" className="font-medium text-violet-700 hover:text-violet-800">
									Forgot password?
								</button>
							</div>

							{error ? (
								<div className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>
							) : null}

							<button
								type="submit"
								disabled={loading}
								className="w-full rounded-lg bg-violet-700 px-4 py-3 text-sm font-semibold text-white shadow-lg shadow-violet-200 transition hover:bg-violet-800 disabled:opacity-60"
							>
								{loading ? 'Signing in...' : 'Sign In to CleanIT'}
							</button>

							<div className="pt-2 text-center text-xs text-slate-500">
								Don&apos;t have an account?{' '}
								<Link to="/register" className="font-semibold text-violet-700 hover:text-violet-800">
									Sign up
								</Link>
							</div>
						</form>
					</div>
				</div>
			</div>
		</div>
	);
}
