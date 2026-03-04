import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import Login from './pages/login/Login';
import Register from './pages/register/Register';
import Dashboard from './pages/dashboard/Dashboard';

export default function App() {
  return (
    <div className="min-h-screen bg-gray-100">
      <Router>
        <nav className="bg-white shadow p-4">
          <ul className="flex space-x-4">
            <li>
              <Link to="/login" className="text-blue-500 hover:underline">
                Login
              </Link>
            </li>
            <li>
              <Link to="/register" className="text-blue-500 hover:underline">
                Register
              </Link>
            </li>
            <li>
              <Link to="/dashboard" className="text-blue-500 hover:underline">
                Dashboard
              </Link>
            </li>
          </ul>
        </nav>
        <main className="p-6">
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />            <Route path="/dashboard" element={<Dashboard />} />            <Route
              path="/"
              element={<div className="text-center mt-10">Welcome to CleanIT!</div>}
            />
          </Routes>
        </main>
      </Router>
    </div>
  );
}
