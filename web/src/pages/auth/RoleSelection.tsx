import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import api from '../../api/axios';

interface OAuthUserData {
  email: string;
  name: string;
  tempToken?: string;
}

export default function RoleSelection() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [userData, setUserData] = useState<OAuthUserData | null>(null);
  const [selectedRole, setSelectedRole] = useState<'client' | 'technician' | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const email = searchParams.get('email');
    const name = searchParams.get('name');
    const tempToken = searchParams.get('tempToken');

    if (!email || !name) {
      navigate('/login?error=missing_oauth_data');
      return;
    }

    setUserData({ email, name, tempToken: tempToken || undefined });
  }, [searchParams, navigate]);

  const handleRoleSubmit = async () => {
    if (!selectedRole || !userData) return;

    setLoading(true);
    setError('');

    try {
      await api.post('/v1/auth/oauth-complete', {
        email: userData.email,
        role: selectedRole,
        tempToken: userData.tempToken
      });

      // Store user info and redirect to appropriate dashboard
      localStorage.setItem('cleanit.user', JSON.stringify({
        email: userData.email,
        name: userData.name,
        role: selectedRole
      }));

      if (selectedRole === 'technician') {
        navigate('/dashboard/technician');
      } else {
        navigate('/dashboard');
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to complete registration');
    } finally {
      setLoading(false);
    }
  };

  if (!userData) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-white">Loading...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full bg-[radial-gradient(circle_at_20%_10%,rgba(124,58,237,0.35),transparent_45%),radial-gradient(circle_at_80%_80%,rgba(99,102,241,0.25),transparent_45%),linear-gradient(135deg,rgba(15,23,42,0.95),rgba(3,7,18,0.95))]">
      <div className="mx-auto flex min-h-screen max-w-6xl flex-col items-center justify-center px-4">
        <div className="mb-8 text-center">
          <div className="text-sm font-semibold tracking-widest text-white/70">ACCOUNT SETUP</div>
          <h1 className="mt-2 text-2xl font-semibold text-white">Welcome, {userData.name}!</h1>
          <p className="mt-2 text-slate-300">Choose your account type to continue</p>
        </div>

        <div className="w-full max-w-md space-y-4">
          <div className="rounded-xl bg-slate-800/50 backdrop-blur-sm p-6 border border-slate-700">
            <div className="mb-4">
              <div className="flex items-center gap-3 mb-4">
                <div className="h-12 w-12 rounded-full bg-violet-600 flex items-center justify-center">
                  <span className="text-white font-semibold">
                    {userData.name.charAt(0).toUpperCase()}
                  </span>
                </div>
                <div>
                  <div className="text-white font-medium">{userData.name}</div>
                  <div className="text-slate-400 text-sm">{userData.email}</div>
                </div>
              </div>
            </div>

            <div className="space-y-3">
              <label className="block">
                <input
                  type="radio"
                  name="role"
                  value="client"
                  checked={selectedRole === 'client'}
                  onChange={(e) => setSelectedRole(e.target.value as 'client')}
                  className="sr-only peer"
                />
                <div className="peer-checked:ring-2 peer-checked:ring-violet-500 peer-checked:bg-violet-600/20 rounded-lg p-4 border border-slate-600 cursor-pointer transition-all hover:bg-slate-700/50">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="text-white font-medium">Client</div>
                      <div className="text-slate-400 text-sm">I need cleaning services</div>
                    </div>
                    <div className="text-2xl">👤</div>
                  </div>
                </div>
              </label>

              <label className="block">
                <input
                  type="radio"
                  name="role"
                  value="technician"
                  checked={selectedRole === 'technician'}
                  onChange={(e) => setSelectedRole(e.target.value as 'technician')}
                  className="sr-only peer"
                />
                <div className="peer-checked:ring-2 peer-checked:ring-violet-500 peer-checked:bg-violet-600/20 rounded-lg p-4 border border-slate-600 cursor-pointer transition-all hover:bg-slate-700/50">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="text-white font-medium">Technician</div>
                      <div className="text-slate-400 text-sm">I provide cleaning services</div>
                    </div>
                    <div className="text-2xl">🔧</div>
                  </div>
                </div>
              </label>
            </div>

            {error && (
              <div className="mt-4 p-3 rounded-lg bg-red-500/20 border border-red-500/50">
                <div className="text-red-400 text-sm">{error}</div>
              </div>
            )}

            <button
              onClick={handleRoleSubmit}
              disabled={!selectedRole || loading}
              className="w-full mt-6 rounded-xl bg-violet-600 px-4 py-3 text-sm font-semibold text-white shadow-lg transition-all hover:bg-violet-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Creating Account...' : 'Continue'}
            </button>

            <div className="mt-4 text-center">
              <button
                onClick={() => navigate('/login')}
                className="text-slate-400 text-sm hover:text-white transition-colors"
              >
                Cancel and return to login
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
