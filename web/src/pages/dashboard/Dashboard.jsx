import React, { useState } from 'react';

const Dashboard = () => {
  const [activeBookings] = useState([]);
  const [userProfile] = useState({
    name: 'John Miller',
    email: 'john@email.com',
    phone: '+63 917 11 0 TT',
    verified: true,
    avatar: 'JM'
  });

  const services = [
    {
      id: 1,
      name: 'Standard External Cleaning',
      description: 'Professional exterior surface cleaning',
      price: '₱200',
      duration: '2 Hours',
      image: 'https://images.unsplash.com/photo-1581578731548-c64695c952952?w=300&h=300&fit=crop'
    },
    {
      id: 2,
      name: 'Deep Internal Cleaning',
      description: 'Comprehensive interior deep clean',
      price: '₱300',
      duration: '3 Hours',
      image: 'https://images.unsplash.com/photo-1581578731548-c64695c952952?w=300&h=300&fit=crop'
    },
    {
      id: 3,
      name: 'GPU Deep Cleaning',
      description: 'Specialized GPU and PC component',
      price: '₱600',
      duration: '1 Hour',
      image: 'https://images.unsplash.com/photo-1581578731548-c64695c952952?w=300&h=300&fit=crop'
    }
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8 flex justify-between items-center">
          <h1 className="text-3xl font-bold text-gray-900">CleanIT</h1>
          <div className="flex items-center gap-4">
            <button className="text-gray-600 hover:text-gray-900">Notifications</button>
            <button className="text-gray-600 hover:text-gray-900">Settings</button>
            <button className="text-gray-600 hover:text-gray-900">Logout</button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 py-10 sm:px-6 lg:px-8">
        {/* Welcome Section */}
        <section className="mb-12">
          <div className="flex justify-between items-start">
            <div>
              <h2 className="text-3xl font-bold text-gray-900 mb-2">Welcome Back!</h2>
              <p className="text-gray-600">Manage your bookings and explore our services</p>
            </div>
            <div className="bg-gradient-to-r from-purple-100 to-pink-100 rounded-xl p-4 border border-purple-200">
              <div className="flex items-center gap-4">
                <div className="bg-gradient-to-br from-purple-500 to-pink-500 text-white rounded-full w-16 h-16 flex items-center justify-center font-bold text-lg">
                  {userProfile.avatar}
                </div>
                <div>
                  <h3 className="font-semibold text-gray-900">{userProfile.name}</h3>
                  <p className="text-sm text-gray-600">{userProfile.email}</p>
                  <p className="text-sm text-gray-600">{userProfile.phone}</p>
                  {userProfile.verified && (
                    <span className="text-xs bg-purple-100 text-purple-700 px-2 py-1 rounded mt-1 inline-block">
                      ✓ Verified
                    </span>
                  )}
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Active Bookings */}
        <section className="mb-12">
          <h3 className="text-2xl font-bold text-gray-900 mb-6">Active Booking</h3>
          {activeBookings.length > 0 ? (
            <div className="grid gap-6">
              {activeBookings.map((booking) => (
                <div key={booking.id} className="bg-white rounded-lg shadow p-6">
                  <h4 className="font-semibold text-lg text-gray-900">{booking.service}</h4>
                  <p className="text-gray-600">{booking.date}</p>
                  <button className="mt-4 text-purple-600 hover:text-purple-700 font-medium">
                    View Details
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <div className="bg-white rounded-lg shadow p-12 text-center">
              <div className="inline-block text-gray-400 mb-4">
                <svg className="w-16 h-16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 7V3m8 4V3m-9 8h18M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
              <h4 className="text-xl font-semibold text-gray-900 mb-2">No Active Bookings</h4>
              <p className="text-gray-600 mb-6">You currently have no active bookings. Verify your account to get started</p>
              <button className="bg-purple-600 text-white px-6 py-2 rounded-lg hover:bg-purple-700 transition">
                Browse Services
              </button>
            </div>
          )}
        </section>

        {/* Browse Services */}
        <section className="mb-12">
          <div className="flex justify-between items-center mb-6">
            <h3 className="text-2xl font-bold text-gray-900">Browse Services</h3>
            <button className="text-purple-600 hover:text-purple-700 font-medium">
              View all Services →
            </button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {services.map((service) => (
              <div
                key={service.id}
                className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition"
              >
                <div className="bg-gradient-to-br from-purple-200 to-pink-200 h-48 flex items-center justify-center">
                  <span className="text-gray-500 text-center">Service Image</span>
                </div>
                <div className="p-6">
                  <h4 className="font-semibold text-lg text-gray-900 mb-2">{service.name}</h4>
                  <p className="text-sm text-gray-600 mb-4">{service.description}</p>
                  <div className="flex justify-between items-center mb-4">
                    <span className="text-lg font-bold text-purple-600">{service.price}</span>
                    <span className="text-xs text-gray-500">{service.duration}</span>
                  </div>
                  <button className="w-full bg-gray-200 text-gray-700 py-2 rounded-lg hover:bg-gray-300 transition font-medium text-sm">
                    Book Now
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Booking History */}
        <section>
          <h3 className="text-2xl font-bold text-gray-900 mb-6">Booking History</h3>
          <div className="bg-white rounded-lg shadow p-12 text-center">
            <div className="inline-block text-gray-400 mb-4">
              <svg className="w-16 h-16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <h4 className="text-xl font-semibold text-gray-900 mb-2">No Booking History</h4>
            <p className="text-gray-600">You haven't made any bookings yet. Verify your account to get started</p>
          </div>
        </section>
      </main>
    </div>
  );
};

export default Dashboard;
