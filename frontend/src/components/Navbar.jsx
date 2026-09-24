import React from 'react';
import { useAuth } from '../context/AuthContext';

export default function Navbar({ currentView, setCurrentView }) {
  const { user, logout, isAdmin } = useAuth();

  const navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: '📊' },
    { id: 'products', label: 'Products', icon: '📦' },
    { id: 'inventory', label: 'Inventory', icon: '📋' },
    { id: 'orders', label: 'Orders', icon: '🛒' },
    { id: 'create-order', label: 'Create Order', icon: '➕' },
  ];

  return (
    <header className="navbar">
      <div className="navbar-container">
        <div className="navbar-brand" onClick={() => setCurrentView('dashboard')}>
          <span className="brand-logo">📦</span>
          <span className="brand-text">InvenTrack</span>
        </div>

        <nav className="navbar-links">
          {navItems.map((item) => (
            <button
              key={item.id}
              className={`nav-link ${currentView === item.id ? 'active' : ''}`}
              onClick={() => setCurrentView(item.id)}
            >
              <span className="nav-icon">{item.icon}</span>
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="navbar-user">
          <div className="user-info">
            <span className="user-name">{user?.username}</span>
            <span className={`role-badge ${isAdmin ? 'role-admin' : 'role-staff'}`}>
              {user?.role}
            </span>
          </div>
          <button className="btn-logout" onClick={logout} title="Sign Out">
            Logout
          </button>
        </div>
      </div>
    </header>
  );
}
