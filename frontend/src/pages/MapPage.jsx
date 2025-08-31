// src/pages/MapPage.jsx
import React, { useState } from 'react';
import { MapContainer, TileLayer, Polyline, Popup, Marker, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { getRecommendedRoutes } from '../services/api';
import PlaceSearchInput from '../components/PlaceSearchInput';

// Fix for default markers in react-leaflet
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

// Custom icons
const startIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const destinationIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

// Component to handle map clicks
function MapClickHandler({ onMapClick }) {
  useMapEvents({
    click(e) {
      onMapClick(e.latlng);
    },
  });
  return null;
}

const MapPage = () => {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [startPlace, setStartPlace] = useState(null);
  const [destinationPlace, setDestinationPlace] = useState(null);
  const [manualClickMode, setManualClickMode] = useState(null);
  const [selectedRoute, setSelectedRoute] = useState(null);
  const [travelMode, setTravelMode] = useState('driving'); // Default to driving
  const [estimatedTimes, setEstimatedTimes] = useState({});

  const handleMapClick = (latlng) => {
    if (!manualClickMode) return;
    
    const place = {
      lat: latlng.lat,
      lon: latlng.lng,
      label: `Selected location (${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)})`
    };
    
    if (manualClickMode === 'start') {
      setStartPlace(place);
    } else {
      setDestinationPlace(place);
    }
    
    setManualClickMode(null);
  };

  // Function to calculate estimated times for different transportation modes
  const calculateEstimatedTimes = (distance) => {
    // Average speeds in km/h
    const speeds = {
      driving: 40, // Account for traffic, lights, etc.
      bicycling: 15,
      walking: 5,
      transit: 25 // Bus/train with stops
    };
    
    const times = {};
    for (const [mode, speed] of Object.entries(speeds)) {
      const timeInHours = distance / 1000 / speed;
      const hours = Math.floor(timeInHours);
      const minutes = Math.round((timeInHours - hours) * 60);
      
      if (hours > 0) {
        times[mode] = `${hours}h ${minutes}m`;
      } else {
        times[mode] = `${minutes}m`;
      }
    }
    
    return times;
  };

  const fetchRecommendedRoutesHandler = async (e) => {
    e.preventDefault();
    
    if (!startPlace || !destinationPlace) {
      setError('Please select both start and destination locations');
      return;
    }

    try {
      setLoading(true);
      setError('');
      setSelectedRoute(null);

      const requestData = {
        originLat: startPlace.lat,
        originLon: startPlace.lon,
        destLat: destinationPlace.lat,
        destLon: destinationPlace.lon,
        travelMode: travelMode // Include selected travel mode
      };

      const response = await getRecommendedRoutes(requestData);

      if (response.success === false) {
        throw new Error(response.message || 'Failed to get routes');
      }

      setRoutes(response.routes || []);
      
      // Calculate estimated times for all transportation modes
      if (response.routes && response.routes.length > 0) {
        const primaryRoute = response.routes[0];
        const estimatedTimes = calculateEstimatedTimes(primaryRoute.distance);
        setEstimatedTimes(estimatedTimes);
      }
    } catch (err) {
      console.error('Route fetch error:', err);
      setError(err.response?.data?.message || 
              err.message || 
              'Failed to calculate routes. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const parsePolyline = (polylineString) => {
    try {
      return JSON.parse(polylineString);
    } catch (e) {
      if (typeof polylineString === 'string' && polylineString.includes(';')) {
        return polylineString.split(';').filter(coord => coord.trim() !== '').map(coord => {
          const [lat, lon] = coord.split(',').map(parseFloat);
          return [lon, lat];
        });
      }
      console.error('Unable to parse polyline:', polylineString);
      return [];
    }
  };

  const formatDuration = (seconds) => {
    const mins = Math.floor(seconds / 60);
    return mins > 60 
      ? `${Math.floor(mins / 60)}h ${mins % 60}m` 
      : `${mins}m`;
  };

  const getMapCenter = () => {
    if (startPlace && destinationPlace) {
      return [
        (startPlace.lat + destinationPlace.lat) / 2,
        (startPlace.lon + destinationPlace.lon) / 2
      ];
    }
    if (startPlace) return [startPlace.lat, startPlace.lon];
    if (destinationPlace) return [destinationPlace.lat, destinationPlace.lon];
    return [12.9716, 77.5946]; // Default to Bangalore
  };

  const getHealthScoreColor = (score) => {
    if (score >= 80) return 'text-green-600 bg-green-100';
    if (score >= 60) return 'text-yellow-600 bg-yellow-100';
    if (score >= 40) return 'text-orange-600 bg-orange-100';
    return 'text-red-600 bg-red-100';
  };

  const getAqiColor = (aqi) => {
    if (aqi <= 50) return 'text-green-600 bg-green-100';
    if (aqi <= 100) return 'text-yellow-600 bg-yellow-100';
    if (aqi <= 150) return 'text-orange-600 bg-orange-100';
    return 'text-red-600 bg-red-100';
  };

  const clearSelection = (type) => {
    if (type === 'start') {
      setStartPlace(null);
    } else {
      setDestinationPlace(null);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <div className="max-w-7xl mx-auto">
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-800">Route Planner</h1>
          <p className="text-gray-600">Find the healthiest route for your journey</p>
        </div>
        
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left panel - Input and results */}
          <div className="lg:col-span-1 space-y-6">
            <div className="bg-white rounded-xl shadow-md p-6">
              <h2 className="text-xl font-semibold text-gray-800 mb-4">Plan Your Route</h2>
              
              <form onSubmit={fetchRecommendedRoutesHandler} className="space-y-4">
                <div>
                  <label htmlFor="start-location" className="block text-sm font-medium text-gray-700 mb-2">
                    Start Location
                  </label>
                  <div className="flex space-x-2">
                    <div className="flex-grow">
                      <PlaceSearchInput
                        id="start-location"
                        onSelect={setStartPlace}
                        value={startPlace}
                        placeholder="Search for start location"
                      />
                    </div>
                    <button
                      type="button"
                      onClick={() => setManualClickMode('start')}
                      className={`px-3 py-2 rounded-lg border ${
                        manualClickMode === 'start' 
                          ? 'bg-blue-100 text-blue-700 border-blue-300' 
                          : 'bg-gray-100 text-gray-700 border-gray-300'
                      }`}
                      title="Select from map"
                    >
                      <i className="fas fa-map-marker-alt"></i>
                    </button>
                    {startPlace && (
                      <button
                        type="button"
                        onClick={() => clearSelection('start')}
                        className="px-3 py-2 rounded-lg bg-red-100 text-red-700 border border-red-300"
                        title="Clear selection"
                      >
                        <i className="fas fa-times"></i>
                      </button>
                    )}
                  </div>
                  {startPlace && (
                    <div className="mt-2 text-sm text-gray-600 bg-blue-50 p-2 rounded-lg">
                      <i className="fas fa-check-circle text-blue-500 mr-2"></i>
                      {startPlace.label}
                    </div>
                  )}
                </div>

                <div>
                  <label htmlFor="destination-location" className="block text-sm font-medium text-gray-700 mb-2">
                    Destination
                  </label>
                  <div className="flex space-x-2">
                    <div className="flex-grow">
                      <PlaceSearchInput
                        id="destination-location"
                        onSelect={setDestinationPlace}
                        value={destinationPlace}
                        placeholder="Search for destination"
                      />
                    </div>
                    <button
                      type="button"
                      onClick={() => setManualClickMode('destination')}
                      className={`px-3 py-2 rounded-lg border ${
                        manualClickMode === 'destination' 
                          ? 'bg-blue-100 text-blue-700 border-blue-300' 
                          : 'bg-gray-100 text-gray-700 border-gray-300'
                      }`}
                      title="Select from map"
                    >
                      <i className="fas fa-map-marker-alt"></i>
                    </button>
                    {destinationPlace && (
                      <button
                        type="button"
                        onClick={() => clearSelection('destination')}
                        className="px-3 py-2 rounded-lg bg-red-100 text-red-700 border border-red-300"
                        title="Clear selection"
                      >
                        <i className="fas fa-times"></i>
                      </button>
                    )}
                  </div>
                  {destinationPlace && (
                    <div className="mt-2 text-sm text-gray-600 bg-blue-50 p-2 rounded-lg">
                      <i className="fas fa-check-circle text-blue-500 mr-2"></i>
                      {destinationPlace.label}
                    </div>
                  )}
                </div>

                {/* Transportation Mode Selector */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Transportation Mode
                  </label>
                  <div className="grid grid-cols-2 gap-2">
                    {['driving', 'transit', 'walking', 'bicycling'].map((mode) => (
                      <button
                        key={mode}
                        type="button"
                        onClick={() => setTravelMode(mode)}
                        className={`p-2 rounded-lg text-center text-sm font-medium ${
                          travelMode === mode
                            ? 'bg-blue-600 text-white'
                            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                      >
                        {mode === 'driving' && '🚗 Driving'}
                        {mode === 'transit' && '🚌 Transit'}
                        {mode === 'walking' && '🚶 Walking'}
                        {mode === 'bicycling' && '🚴 Cycling'}
                      </button>
                    ))}
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading || !startPlace || !destinationPlace}
                  className="w-full bg-blue-600 text-white py-3 rounded-lg font-medium hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors flex items-center justify-center"
                >
                  {loading ? (
                    <>
                      <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                      Calculating Routes...
                    </>
                  ) : (
                    'Find Routes'
                  )}
                </button>
              </form>

              {error && (
                <div className="mt-4 p-3 text-red-700 bg-red-50 rounded-lg border border-red-200">
                  <i className="fas fa-exclamation-circle mr-2"></i>
                  {error}
                </div>
              )}

              {/* Estimated Times for Different Modes */}
              {Object.keys(estimatedTimes).length > 0 && (
                <div className="mt-6">
                  <h3 className="text-lg font-medium text-gray-800 mb-3">Estimated Travel Times</h3>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="bg-gray-50 p-3 rounded-lg">
                      <div className="flex items-center">
                        <span className="text-2xl mr-2">🚗</span>
                        <div>
                          <p className="text-sm font-medium">Driving</p>
                          <p className="text-lg font-semibold">{estimatedTimes.driving}</p>
                        </div>
                      </div>
                    </div>
                    <div className="bg-gray-50 p-3 rounded-lg">
                      <div className="flex items-center">
                        <span className="text-2xl mr-2">🚌</span>
                        <div>
                          <p className="text-sm font-medium">Transit</p>
                          <p className="text-lg font-semibold">{estimatedTimes.transit}</p>
                        </div>
                      </div>
                    </div>
                    <div className="bg-gray-50 p-3 rounded-lg">
                      <div className="flex items-center">
                        <span className="text-2xl mr-2">🚶</span>
                        <div>
                          <p className="text-sm font-medium">Walking</p>
                          <p className="text-lg font-semibold">{estimatedTimes.walking}</p>
                        </div>
                      </div>
                    </div>
                    <div className="bg-gray-50 p-3 rounded-lg">
                      <div className="flex items-center">
                        <span className="text-2xl mr-2">🚴</span>
                        <div>
                          <p className="text-sm font-medium">Cycling</p>
                          <p className="text-lg font-semibold">{estimatedTimes.bicycling}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {routes.length > 0 && (
              <div className="bg-white rounded-xl shadow-md p-6">
                <h2 className="text-xl font-semibold text-gray-800 mb-4">Recommended Routes</h2>
                <div className="space-y-4">
                  {routes.map((route, index) => (
                    <div 
                      key={route.routeId || index} 
                      className={`p-4 border rounded-lg cursor-pointer transition-all ${
                        selectedRoute === index 
                          ? 'border-blue-500 bg-blue-50 shadow-md' 
                          : 'border-gray-200 hover:border-blue-300 hover:shadow-sm'
                      }`}
                      onClick={() => setSelectedRoute(index)}
                    >
                      <div className="flex justify-between items-start">
                        <h3 className="font-bold text-lg text-gray-800">Option {index + 1}</h3>
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${getHealthScoreColor(route.healthScore)}`}>
                          Health Score: {route.healthScore.toFixed(0)}
                        </span>
                      </div>
                      <div className="mt-3 space-y-2 text-sm">
                        <div className="flex justify-between">
                          <span className="text-gray-600">Distance:</span>
                          <span className="font-medium">{(route.distance / 1000).toFixed(2)} km</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-600">Duration:</span>
                          <span className="font-medium">{formatDuration(route.duration)}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-600">AQI:</span>
                          <span className={`px-2 py-1 rounded-full text-xs font-medium ${getAqiColor(route.aqi)}`}>
                            {route.aqi}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Right panel - Map */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-xl shadow-md p-4">
              <div className="h-96 w-full rounded-lg overflow-hidden">
                <MapContainer
                  center={getMapCenter()}
                  zoom={13}
                  style={{ height: '100%', width: '100%' }}
                >
                  <TileLayer
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                  />
                  
                  <MapClickHandler onMapClick={handleMapClick} />
                  
                  {startPlace && (
                    <Marker position={[startPlace.lat, startPlace.lon]} icon={startIcon}>
                      <Popup>
                        <div className="font-medium">Start Location</div>
                        <div className="text-sm text-gray-600">{startPlace.label}</div>
                      </Popup>
                    </Marker>
                  )}
                  
                  {destinationPlace && (
                    <Marker position={[destinationPlace.lat, destinationPlace.lon]} icon={destinationIcon}>
                      <Popup>
                        <div className="font-medium">Destination</div>
                        <div className="text-sm text-gray-600">{destinationPlace.label}</div>
                      </Popup>
                    </Marker>
                  )}

                  {routes.map((route, index) => {
                    const coordinates = parsePolyline(route.polyline);
                    
                    if (coordinates.length === 0) return null;
                    
                    const leafletCoords = coordinates.map(([lon, lat]) => [lat, lon]);
                    const isSelected = selectedRoute === index;
                    
                    return (
                      <Polyline
                        key={route.routeId || index}
                        positions={leafletCoords}
                        color={isSelected ? '#3b82f6' : '#9ca3af'}
                        weight={isSelected ? 6 : 4}
                        opacity={isSelected ? 0.9 : 0.6}
                      >
                        <Popup>
                          <div className="font-medium">Option {index + 1}</div>
                          <div className="text-sm">
                            <p>Health Score: {route.healthScore.toFixed(0)}</p>
                            <p>Distance: {(route.distance / 1000).toFixed(2)} km</p>
                            <p>AQI: {route.aqi}</p>
                          </div>
                        </Popup>
                      </Polyline>
                    );
                  })}
                </MapContainer>
              </div>
              
              {manualClickMode && (
                <div className="mt-4 p-3 bg-blue-50 text-blue-700 rounded-lg border border-blue-200">
                  <i className="fas fa-info-circle mr-2"></i>
                  Click on the map to set {manualClickMode === 'start' ? 'start' : 'destination'} location
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default MapPage;