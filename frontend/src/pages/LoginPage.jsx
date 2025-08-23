// src/pages/LoginPage.jsx
import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../services/api';

const LoginPage = ({ setIsAuthenticated, setUserProfile }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      // First, login to get the token
      const loginResponse = await login(email, password);
      
      if (loginResponse.token) {
        // Set authentication state immediately
        setIsAuthenticated(true);
        
        // Try to get profile, but don't let it block the login flow
        try {
          // We'll let the App component handle profile loading
          // This prevents the login page from getting stuck on profile errors
          navigate('/');
        } catch (profileError) {
          console.error('Profile fetch failed, but login was successful:', profileError);
          // Still navigate to home, the app will handle the profile loading
          navigate('/');
        }
      } else {
        setError('Login failed. No token received.');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid email or password. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto p-6 bg-white rounded-lg shadow mt-8">
      <h2 className="text-2xl font-bold mb-6 text-center">Login</h2>
      
      {error && <div className="text-red-500 mb-4 p-3 bg-red-50 rounded">{error}</div>}
      
      <form onSubmit={handleSubmit}>
        <div className="mb-4">
          <label htmlFor="email" className="block mb-2">Email</label>
          <input
            type="email"
            id="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="w-full px-3 py-2 border rounded"
          />
        </div>
        
        <div className="mb-4">
          <label htmlFor="password" className="block mb-2">Password</label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="w-full px-3 py-2 border rounded"
          />
        </div>
        
        <button 
          type="submit"
          disabled={loading}
          className="w-full bg-green-600 text-white py-2 rounded hover:bg-green-700 disabled:bg-gray-400"
        >
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>
      
      <div className="mt-4 text-center">
        <Link to="/register" className="text-green-600 hover:underline">
          Don't have an account? Register
        </Link>
      </div>
    </div>
  );
};

export default LoginPage;