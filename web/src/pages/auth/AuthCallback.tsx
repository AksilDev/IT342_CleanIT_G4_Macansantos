import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { supabase } from '../../api/supabaseClient';
import api from '../../api/axios';

export default function AuthCallback() {
  const navigate = useNavigate();

  useEffect(() => {
    async function handleCallback() {
      // Supabase reads the token from the URL hash automatically
      const { data: { session }, error } = await supabase.auth.getSession();

      if (error || !session) {
        navigate('/login?error=oauth_failed');
        return;
      }

      const { email, user_metadata } = session.user;
      const name = user_metadata?.full_name || user_metadata?.name || 'Google User';
      const supabaseToken = session.access_token;

      try {
        // Check if user already exists in your Spring Boot DB
        const res = await api.post('/v1/auth/oauth-check', { email }, {
          headers: { Authorization: `Bearer ${supabaseToken}` }
        });

        const { exists, role, token } = res.data;

        if (exists) {
          // Existing user — save token and go to dashboard
          localStorage.setItem('cleanit.token', token);
          localStorage.setItem('cleanit.user', JSON.stringify({ email, name, role }));
          if (role === 'technician') navigate('/dashboard/technician');
          else if (role === 'admin') navigate('/admin/dashboard');
          else navigate('/dashboard');
        } else {
          // New user — go to role selection
          navigate(`/role-selection?email=${encodeURIComponent(email!)}&name=${encodeURIComponent(name)}&supabaseToken=${supabaseToken}`);
        }
      } catch {
        navigate('/login?error=oauth_failed');
      }
    }

    handleCallback();
  }, [navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-neutral-900">
      <div className="text-center">
        <div className="text-white text-lg font-medium">Signing you in...</div>
        <div className="mt-3 h-1.5 w-40 rounded-full bg-slate-700 overflow-hidden mx-auto">
          <div className="h-full bg-violet-500 rounded-full animate-pulse w-2/3"></div>
        </div>
      </div>
    </div>
  );
}
