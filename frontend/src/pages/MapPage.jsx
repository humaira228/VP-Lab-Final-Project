// src/pages/MapPage.jsx
import React, { useState } from 'react';
import { MapContainer, TileLayer, Polyline, Popup, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { getRecommendedRoutes } from '../services/api';
import PlaceSearchInput from '../components/PlaceSearchInput';

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
  const [manualClickMode, setManualClickMode] = useState(null); // 'start' or 'destination'

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

  const fetchRecommendedRoutesHandler = async (e) => {
    e.preventDefault();
    
    // Validate inputs
    if (!startPlace || !destinationPlace) {
      setError('Please select both start and destination locations');
      return;
    }

    try {
      setLoading(true);
      setError('');

      const requestData = {
        originLat: startPlace.lat,
        originLon: startPlace.lon,
        destLat: destinationPlace.lat,
        destLon: destinationPlace.lon
      };

      const response = await getRecommendedRoutes(requestData);

      if (response.success === false) {
        throw new Error(response.message || 'Failed to get routes');
      }

      setRoutes(response.routes || []);
    } catch (err) {
      console.error('Route fetch error:', err);
      setError(err.response?.data?.message || 
              err.message || 
              'Failed to calculate routes. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Helper function to parse polyline
  const parsePolyline = (polylineString) => {
    try {
      // Try to parse as JSON first
      return JSON.parse(polylineString);
    } catch (e) {
      // If it's not JSON, try to parse as semicolon-separated string
      if (typeof polylineString === 'string' && polylineString.includes(';')) {
        return polylineString.split(';').filter(coord => coord.trim() !== '').map(coord => {
          const [lat, lon] = coord.split(',').map(parseFloat);
          return [lon, lat]; // Return as [lon, lat] to match expected format
        });
      }
      console.error('Unable to parse polyline:', polylineString);
      return [];
    }
  };

  // Helper function to format duration
  const formatDuration = (seconds) => {
    const mins = Math.floor(seconds / 60);
    return mins > 60 
      ? `${Math.floor(mins / 60)}h ${mins % 60}m` 
      : `${mins}m`;
  };

  // Get center for map
  const getMapCenter = () => {
    if (startPlace) return [startPlace.lat, startPlace.lon];
    if (destinationPlace) return [destinationPlace.lat, destinationPlace.lon];
    return [12.9716, 77.5946]; // Default to Bangalore
  };

  return (
    <div className="p-4 max-w-6xl mx-auto">
      <h1 className="text-2xl font-bold mb-4">Route Planner</h1>
      
      <form onSubmit={fetchRecommendedRoutesHandler} className="mb-6 p-4 bg-gray-50 rounded-lg">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
          <div>
            <label htmlFor="start-location" className="block mb-2 font-medium">Start Location</label>
            <PlaceSearchInput
              id="start-location"
              label=""
              onSelect={setStartPlace}
              value={startPlace}
              placeholder="Search for start location"
            />
            <div className="mt-2">
              <button
                type="button"
                onClick={() => setManualClickMode('start')}
                className={`text-sm px-3 py-1 rounded ${
                  manualClickMode === 'start' 
                    ? 'bg-blue-600 text-white' 
                    : 'bg-gray-200 text-gray-800'
                }`}
              >
                {manualClickMode === 'start' ? 'Click on map...' : 'Select from map'}
              </button>
            </div>
            {startPlace && (
              <div className="mt-2 text-sm text-gray-600">
                Selected: {startPlace.label}
              </div>
            )}
          </div>

          <div>
            <label htmlFor="destination-location" className="block mb-2 font-medium">Destination</label>
            <PlaceSearchInput
              id="destination-location"
              label=""
              onSelect={setDestinationPlace}
              value={destinationPlace}
              placeholder="Search for destination"
            />
            <div className="mt-2">
              <button
                type="button"
                onClick={() => setManualClickMode('destination')}
                className={`text-sm px-3 py-1 rounded ${
                  manualClickMode === 'destination' 
                    ? 'bg-blue-600 text-white' 
                    : 'bg-gray-200 text-gray-800'
                }`}
              >
                {manualClickMode === 'destination' ? 'Click on map...' : 'Select from map'}
              </button>
            </div>
            {destinationPlace && (
              <div className="mt-2 text-sm text-gray-600">
                Selected: {destinationPlace.label}
              </div>
            )}
          </div>
        </div>

        <button
          type="submit"
          disabled={loading || !startPlace || !destinationPlace}
          className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 disabled:bg-gray-400"
        >
          {loading ? 'Calculating...' : 'Find Routes'}
        </button>
      </form>

      {error && (
        <div className="p-4 mb-4 text-red-600 bg-red-50 rounded-lg">
          {error}
        </div>
      )}

      <div className="h-96 w-full rounded-lg overflow-hidden border mb-6">
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
            <Popup position={[startPlace.lat, startPlace.lon]}>
              <div>Start Location</div>
            </Popup>
          )}
          
          {destinationPlace && (
            <Popup position={[destinationPlace.lat, destinationPlace.lon]}>
              <div>Destination</div>
            </Popup>
          )}

          {routes.map((route, index) => {
            const coordinates = parsePolyline(route.polyline);
            
            if (coordinates.length === 0) {
              console.warn('No coordinates for route:', route.routeId || index);
              return null;
            }
            
            // Convert from [lon, lat] to [lat, lon] for Leaflet
            const leafletCoords = coordinates.map(([lon, lat]) => [lat, lon]);
            
            return (
              <Polyline
                key={route.routeId || index}
                positions={leafletCoords}
                color={route.color || '#3b82f6'}
                weight={5}
              >
                <Popup>
                  <div>
                    <p className="font-bold">Health Score: {route.healthScore.toFixed(0)}</p>
                    <p>Distance: {(route.distance / 1000).toFixed(2)} km</p>
                    <p>AQI: {route.aqi}</p>
                  </div>
                </Popup>
              </Polyline>
            );
          })}
        </MapContainer>
      </div>

      {routes.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {routes.map((route, index) => (
            <div key={route.routeId || index} className="p-4 border rounded-lg bg-white shadow-sm">
              <h3 className="font-bold text-lg mb-2">Option {index + 1}</h3>
              <div className="space-y-2">
                <p>Distance: {(route.distance / 1000).toFixed(2)} km</p>
                <p>Duration: {formatDuration(route.duration)}</p>
                <div className="flex items-center">
                  <span>Health Score: </span>
                  <span className={`ml-2 font-bold ${
                    route.healthScore >= 70 ? 'text-green-600' :
                    route.healthScore >= 40 ? 'text-yellow-600' : 'text-red-600'
                  }`}>
                    {route.healthScore.toFixed(0)}
                  </span>
                </div>
                <div className="flex items-center">
                  <span>AQI: </span>
                  <span className={`ml-2 font-bold ${
                    route.aqi <= 50 ? 'text-green-600' :
                    route.aqi <= 100 ? 'text-yellow-600' : 'text-red-600'
                  }`}>
                    {route.aqi}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default MapPage;