// src/pages/HomePage.jsx
import React from 'react';
import { Link } from 'react-router-dom';

const HomePage = () => {
  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold text-gray-800 mb-4">Plan Healthier Routes</h1>
        <p className="text-xl text-gray-600 max-w-3xl mx-auto">
          EcoTrack helps you find the healthiest walking routes by considering air quality,
          distance, and your personal health profile.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-12">
        <div className="bg-white p-6 rounded-lg shadow-md border border-green-100">
          <div className="text-green-500 text-3xl mb-4">🌍</div>
          <h3 className="text-xl font-semibold mb-2">Air Quality Aware</h3>
          <p className="text-gray-600">
            Routes are evaluated based on real-time air quality data to minimize your
            exposure to pollution.
          </p>
        </div>
        
        <div className="bg-white p-6 rounded-lg shadow-md border border-green-100">
          <div className="text-green-500 text-3xl mb-4">❤️</div>
          <h3 className="text-xl font-semibold mb-2">Personalized Health</h3>
          <p className="text-gray-600">
            Customize routes based on your health profile and sensitivity to pollution.
          </p>
        </div>
        
        <div className="bg-white p-6 rounded-lg shadow-md border border-green-100">
          <div className="text-green-500 text-3xl mb-4">🚶</div>
          <h3 className="text-xl font-semibold mb-2">Multiple Options</h3>
          <p className="text-gray-600">
            Get multiple route alternatives with health scores to choose what works best.
          </p>
        </div>
      </div>

      <div className="text-center">
        <Link 
          to="/map" 
          className="inline-block bg-green-600 hover:bg-green-700 text-white font-bold py-3 px-6 rounded-lg transition duration-300"
        >
          Start Exploring Routes
        </Link>
      </div>
    </div>
  );
};

export default HomePage;