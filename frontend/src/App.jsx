import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import ProductsPage from './pages/ProductsPage';
import InventoryPage from './pages/InventoryPage';
import OrdersPage from './pages/OrdersPage';
import CreateOrderPage from './pages/CreateOrderPage';

function AppContent() {
  const { isAuthenticated } = useAuth();
  const [authView, setAuthView] = useState('login'); // 'login' | 'register'
  const [currentView, setCurrentView] = useState('dashboard'); // 'dashboard' | 'products' | 'inventory' | 'orders' | 'create-order'

  // If unauthenticated, show Login or Register
  if (!isAuthenticated) {
    if (authView === 'register') {
      return <RegisterPage onNavigateLogin={() => setAuthView('login')} />;
    }
    return <LoginPage onNavigateRegister={() => setAuthView('register')} />;
  }

  // Render appropriate view based on currentView
  const renderCurrentView = () => {
    switch (currentView) {
      case 'dashboard':
        return <DashboardPage setCurrentView={setCurrentView} />;
      case 'products':
        return <ProductsPage />;
      case 'inventory':
        return <InventoryPage />;
      case 'orders':
        return <OrdersPage setCurrentView={setCurrentView} />;
      case 'create-order':
        return <CreateOrderPage setCurrentView={setCurrentView} />;
      default:
        return <DashboardPage setCurrentView={setCurrentView} />;
    }
  };

  return (
    <div className="app-layout">
      <Navbar currentView={currentView} setCurrentView={setCurrentView} />
      <main className="main-content">{renderCurrentView()}</main>
      <footer className="app-footer">
        InvenTrack System • Connected to Spring Boot Backend API ([http://localhost:8080](http://localhost:8080/))
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
