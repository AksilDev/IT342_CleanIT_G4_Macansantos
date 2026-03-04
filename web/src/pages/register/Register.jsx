import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from '../../api/axios';

const Register = () => {
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: ''
  });
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(false);
  const [agreedToTerms, setAgreedToTerms] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!agreedToTerms) {
      setMessage('You must agree to the Terms and Conditions');
      return;
    }
    setLoading(true);
    try {
      await axios.post('/api/v1/auth/register', form, {
        headers: { 'Content-Type': 'application/json' },
      });
      setMessage('Registered successfully! Redirecting to login...');
      setLoading(false);
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      setMessage(err?.response?.data?.message || 'Registration failed');
      setLoading(false);
    }
  };

  return (
    <div
      className="min-h-screen flex items-center justify-center bg-cover bg-center py-10"
      style={{
        backgroundImage:
          'url("https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=1200&h=800&fit=crop")',
      }}
    >
      <div className="absolute inset-0 bg-black opacity-40"></div>
      <div className="relative z-10 w-full max-w-2xl mx-4">
        <div className="bg-white rounded-2xl p-10 shadow-2xl">
          <div className="text-center mb-8">
            <p className="text-sm text-gray-500 mb-2">Client Registration</p>
            <h1 className="text-3xl font-bold text-gray-900 mb-4">
              Create Your CleanIT Account
            </h1>
            <div className="flex justify-center gap-4 mb-4">
              <div className="w-2 h-2 bg-purple-600 rounded-full"></div>
              <div className="w-8 h-0.5 bg-purple-600"></div>
              <div className="w-2 h-2 bg-purple-600 rounded-full"></div>
              <div className="w-8 h-0.5 bg-purple-600"></div>
              <div className="w-2 h-2 bg-purple-600 rounded-full"></div>
            </div>
            <p className="text-sm text-gray-600">
              Join CleanIT{' '}
              <Link to="/login" className="text-purple-600 hover:text-purple-700 font-semibold">
                Already have an account? Sign in here
              </Link>
            </p>
          </div>

          {message && (
            <div className={`mb-6 p-4 rounded-lg ${
              message.includes('successfully')
                ? 'bg-green-100 text-green-700'
                : 'bg-red-100 text-red-700'
            }`}>
              {message}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="bg-gray-50 p-6 rounded-xl">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Registration Progress
              </h2>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Name
                  </label>
                  <input
                    type="text"
                    name="fullName"
                    placeholder="Enter your name"
                    value={form.fullName}
                    onChange={handleChange}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 placeholder-gray-400"
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-4 col-span-2">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Email
                    </label>
                    <input
                      type="email"
                      name="email"
                      placeholder="Enter your email"
                      value={form.email}
                      onChange={handleChange}
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 placeholder-gray-400"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Password
                    </label>
                    <input
                      type="password"
                      name="password"
                      placeholder="Enter your password"
                      value={form.password}
                      onChange={handleChange}
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 placeholder-gray-400"
                      required
                    />
                  </div>
                </div>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <input
                type="checkbox"
                checked={agreedToTerms}
                onChange={(e) => setAgreedToTerms(e.target.checked)}
                className="w-5 h-5 text-purple-600 rounded focus:ring-purple-500 mt-1"
              />
              <label className="text-sm text-gray-700">
                By signing up, you agree to our{' '}
                <Link to="/terms" className="text-purple-600 hover:text-purple-700 font-semibold">
                  Terms and Conditions
                </Link>
              </label>
            </div>

            <button
              type="submit"
              disabled={loading || !agreedToTerms}
              className="w-full bg-gradient-to-r from-purple-600 to-purple-700 text-white font-semibold py-3 rounded-lg hover:from-purple-700 hover:to-purple-800 transition-all duration-200 disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {loading ? 'Processing...' : 'Complete Form'} →
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Register;
