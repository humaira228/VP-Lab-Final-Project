import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import MapPage from './pages/MapPage';
import ProfilePage from './pages/ProfilePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';

export default function App() {
    const [isAuthenticated, setIsAuthenticated] = useState(false);

    useEffect(() => {
        // Check authentication on first load
        const token = localStorage.getItem('jwtToken');
        setIsAuthenticated(!!token);
    }, []);

    return (
        <Router>
            <div className="min-h-screen flex flex-col bg-gray-50">
                {/* Navbar always visible */}
                <Navbar isAuthenticated={isAuthenticated} />

                {/* Main content */}
                <main className="flex-grow">
                    <Routes>
                        <Route path="/" element={<HomePage />} />
                        <Route path="/map" element={<MapPage />} />
                        <Route path="/profile" element={<ProfilePage />} />
                        <Route
                            path="/login"
                            element={<LoginPage setIsAuthenticated={setIsAuthenticated} />}
                        />
                        <Route path="/register" element={<RegisterPage />} />
                    </Routes>
                </main>

                {/* Footer */}
                <footer className="bg-gray-800 text-white py-4">
                    <div className="max-w-7xl mx-auto px-4 text-center">
                        © {new Date().getFullYear()} EcoTrack - Sustainable Route Planning
                    </div>
                </footer>
            </div>
        </Router>
    );
}
