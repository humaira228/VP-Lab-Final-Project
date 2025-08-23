// src/App.jsx
import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import MapPage from './pages/MapPage';
import ProfilePage from './pages/ProfilePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import RouteDetailPage from './pages/RouteDetailPage';
import { isAuthenticated, getProfile } from './services/api';

export default function App() {
  const [isAuth, setIsAuth] = useState(false);
  const [userProfile, setUserProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const checkAuthStatus = async () => {
      const hasToken = isAuthenticated();
      
      if (hasToken) {
        try {
          // Try to get profile to validate token
          const profile = await getProfile();
          setUserProfile(profile);
          setIsAuth(true);
        } catch (error) {
          console.error('Token validation failed:', error);
          // Token is invalid, clear it
          localStorage.removeItem('jwtToken');
          localStorage.removeItem('userProfile');
          setIsAuth(false);
        }
      } else {
        // Check if we have user profile in localStorage
        const storedProfile = localStorage.getItem('userProfile');
        if (storedProfile) {
          setUserProfile(JSON.parse(storedProfile));
        }
      }
      
      setLoading(false);
    };

    checkAuthStatus();
  }, []);

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center">Loading...</div>;
  }

  return (
    <Router>
      <div className="min-h-screen flex flex-col bg-gray-50">
        <Navbar 
          isAuthenticated={isAuth} 
          userProfile={userProfile}
          setIsAuthenticated={setIsAuth}
          setUserProfile={setUserProfile}
        />
        
        <main className="flex-grow container mx-auto px-4 py-8">
          <Routes>
            <Route path="/" element={<HomePage userProfile={userProfile} />} />
            <Route path="/map" element={<MapPage userProfile={userProfile} />} />
            <Route 
              path="/profile" 
              element={<ProfilePage 
                profile={userProfile} 
                setProfile={setUserProfile}
                setIsAuthenticated={setIsAuth}
              />} 
            />
            <Route
              path="/login"
              element={<LoginPage setIsAuthenticated={setIsAuth} setUserProfile={setUserProfile} />}
            />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/route/:id" element={<RouteDetailPage />} />
          </Routes>
        </main>
        
        <footer className="bg-gray-800 text-white py-6">
          <div className="container mx-auto px-4">
            <div className="flex flex-col md:flex-row justify-between items-center">
              <div className="mb-4 md:mb-0">
                <h3 className="text-xl font-bold flex items-center">
                  <span className="mr-2">🌿</span> EcoTrack
                </h3>
                <p className="text-gray-400">Health-first navigation system</p>
              </div>
              
              <div className="flex space-x-4">
                <a href="#" className="hover:text-green-400 transition">About</a>
                <a href="#" className="hover:text-green-400 transition">Contact</a>
                <a href="#" className="hover:text-green-400 transition">Privacy</a>
              </div>
            </div>
            
            <div className="mt-6 text-center text-gray-400 text-sm">
              © {new Date().getFullYear()} EcoTrack. All rights reserved.
            </div>
          </div>
        </footer>
      </div>
    </Router>
  );
}