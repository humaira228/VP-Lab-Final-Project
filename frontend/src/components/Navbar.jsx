// src/components/Navbar.jsx
import React, { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { logout } from '../services/api';

const Navbar = ({ isAuthenticated, userProfile, setIsAuthenticated, setUserProfile }) => {
  const [isOpen, setIsOpen] = useState(false);
  const navigate = useNavigate();
  const menuRef = useRef(null);

  // Close menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleLogout = () => {
    logout();
    setIsAuthenticated(false);
    setUserProfile(null);
    navigate('/login');
    setIsOpen(false);
  };

  const closeMenu = () => {
    setIsOpen(false);
  };

  return (
    <nav className="bg-green-600 text-white shadow-lg">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex justify-between h-16">
          <div className="flex items-center">
            <Link to="/" className="text-xl font-bold flex items-center">
              <span className="mr-2">🌿</span>
              EcoTrack
            </Link>
          </div>
          
          {/* User info when authenticated */}
          {isAuthenticated && userProfile && (
            <div className="hidden md:flex items-center mr-4">
              <span className="mr-4">Welcome, {userProfile.firstName || userProfile.email}</span>
            </div>
          )}
          
          {/* Desktop Menu */}
          <div className="hidden md:flex items-center space-x-4">
            <Link to="/" className="px-3 py-2 rounded-md hover:bg-green-700 transition-colors duration-200">Home</Link>
            <Link to="/map" className="px-3 py-2 rounded-md hover:bg-green-700 transition-colors duration-200">Map</Link>
            <Link to="/profile" className="px-3 py-2 rounded-md hover:bg-green-700 transition-colors duration-200">Profile</Link>
            {isAuthenticated ? (
              <button 
                onClick={handleLogout}
                className="px-3 py-2 rounded-md bg-red-500 hover:bg-red-600 transition-colors duration-200"
              >
                Logout
              </button>
            ) : (
              <Link to="/login" className="px-3 py-2 rounded-md bg-blue-500 hover:bg-blue-600 transition-colors duration-200">Login</Link>
            )}
          </div>
          
          {/* Mobile Menu Button */}
          <div className="md:hidden flex items-center">
            <button 
              onClick={() => setIsOpen(!isOpen)}
              className="text-white focus:outline-none p-2"
              aria-label="Toggle menu"
            >
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                {isOpen ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                )}
              </svg>
            </button>
          </div>
        </div>
      </div>
      
      {/* Mobile Menu */}
      {isOpen && (
        <div ref={menuRef} className="md:hidden bg-green-600 absolute w-full z-50 shadow-lg">
          <div className="px-2 pt-2 pb-3 space-y-1">
            {isAuthenticated && userProfile && (
              <div className="px-3 py-2 text-green-200 border-b border-green-500">
                Welcome, {userProfile.firstName || userProfile.email}
              </div>
            )}
            <Link 
              to="/" 
              className="block px-3 py-2 rounded-md hover:bg-green-700 transition-colors duration-200"
              onClick={closeMenu}
            >
              Home
            </Link>
            <Link 
              to="/map" 
              className="block px-3 py-2 rounded-md hover:bg-green-700 transition-colors duration-200"
              onClick={closeMenu}
            >
              Map
            </Link>
            <Link 
              to="/profile" 
              className="block px-3 py-2 rounded-md hover:bg-green-700 transition-colors duration-200"
              onClick={closeMenu}
            >
              Profile
            </Link>
            {isAuthenticated ? (
              <button 
                onClick={handleLogout}
                className="w-full text-left block px-3 py-2 rounded-md bg-red-500 hover:bg-red-600 transition-colors duration-200"
              >
                Logout
              </button>
            ) : (
              <Link 
                to="/login" 
                className="block px-3 py-2 rounded-md bg-blue-500 hover:bg-blue-600 transition-colors duration-200"
                onClick={closeMenu}
              >
                Login
              </Link>
            )}
          </div>
        </div>
      )}
    </nav>
  );
};

export default Navbar;