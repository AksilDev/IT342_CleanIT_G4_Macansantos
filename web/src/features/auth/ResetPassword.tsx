import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import api from '../../shared/api/axios';

export default function ResetPassword() {
	const navigate = useNavigate();
	const [searchParams] = useSearchParams();
	const [token, setToken] = useState(searchParams.get('token') ?? '');
	const [newPassword, setNewPassword] = useState('');
	const [confirmPassword, setConfirmPassword] = useState('');
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [success, setSuccess] = useState<string | null>(null);

	async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
		e.preventDefault();
		setError(null);
		setSuccess(null);

		if (!token.trim()) {
			setError('Reset token is required.');
			return;
		}
		if (newPassword !== confirmPassword) {
			setError('Passwords do not match.');
			return;
		}

		setLoading(true);
		try {
			const res = await api.post('/v1/auth/reset-password', {
				token: token.trim(),
				newPassword,
			});
			setSuccess(res.data?.message ?? 'Password reset successful.');
			setTimeout(() => navigate('/login'), 2000);
		} catch (err: any) {
			setError(err?.response?.data?.message ?? 'Failed to reset password.');
		} finally {
			setLoading(false);
		}
	}

	return (
		<div className="min-h-screen w-full bg-neutral-900">
			<div className="mx-auto flex min-h-screen max-w-md flex-col items-center justify-center px-4">
				<div className="w-full rounded-3xl bg-white p-8 shadow-2xl">
					<h1 className="text-2xl font-bold text-slate-900">Reset password</h1>
					<p className="mt-2 text-sm text-slate-500">
						Enter the reset token from your request and choose a new password.
					</p>

					<form className="mt-6 space-y-4" onSubmit={onSubmit}>
						<div>
							<label className="text-sm font-medium text-slate-700">Reset token</label>
							<input
								value={token}
								onChange={(e) => setToken(e.target.value)}
								className="mt-2 w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm"
								required
							/>
						</div>
						<div>
							<label className="text-sm font-medium text-slate-700">New password</label>
							<input
								type="password"
								value={newPassword}
								onChange={(e) => setNewPassword(e.target.value)}
								className="mt-2 w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm"
								required
							/>
							<p className="mt-1 text-xs text-slate-400">Min 8 chars, 1 uppercase, 1 number</p>
						</div>
						<div>
							<label className="text-sm font-medium text-slate-700">Confirm password</label>
							<input
								type="password"
								value={confirmPassword}
								onChange={(e) => setConfirmPassword(e.target.value)}
								className="mt-2 w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm"
								required
							/>
						</div>

						{error && (
							<div className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>
						)}
						{success && (
							<div className="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{success}</div>
						)}

						<button
							type="submit"
							disabled={loading}
							className="w-full rounded-lg bg-violet-700 px-4 py-3 text-sm font-semibold text-white hover:bg-violet-800 disabled:opacity-60"
						>
							{loading ? 'Resetting...' : 'Reset password'}
						</button>
					</form>

					<p className="mt-4 text-center text-sm text-slate-500">
						<Link to="/login" className="font-semibold text-violet-700">
							Back to login
						</Link>
					</p>
				</div>
			</div>
		</div>
	);
}
