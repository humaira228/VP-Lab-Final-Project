// src/pages/RouteDetailPage.jsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Polyline, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { getRouteDetails } from '../services/api';

const RouteDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [route, setRoute] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchRoute = async () => {
      try {
        const response = await getRouteDetails(id);
        setRoute(response);
      } catch (err) {
        setError(err.message || 'Failed to load route');
      } finally {
        setLoading(false);
      }
    };

    fetchRoute();
  }, [id]);

  if (loading) return <div className="p-4">Loading route details...</div>;
  if (error) return <div className="p-4 text-red-500">Error: {error}</div>;
  if (!route) return <div className="p-4">Route not found</div>;

  const center = route.polyline?.length > 0 
    ? [route.polyline[0][1], route.polyline[0][0]] 
    : [12.9716, 77.5946];

  return (
    <div className="space-y-6 p-4">
      <button 
        onClick={() => navigate(-1)}
        className="flex items-center text-blue-600 hover:text-blue-800"
      >
        ← Back to all routes
      </button>

      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-2xl font-bold mb-4">Route Details</h2>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h3 className="text-lg font-semibold mb-2">Route Information</h3>
            <div className="space-y-3">
              <div>
                <span className="font-medium">Distance:</span> {(route.distance / 1000).toFixed(2)} km
              </div>
              <div>
                <span className="font-medium">Duration:</span> {Math.floor(route.duration / 60)} minutes
              </div>
              <div>
                <span className="font-medium">Health Score:</span> 
                <span className={`ml-2 font-bold ${
                  route.healthScore >= 70 ? 'text-green-500' :
                  route.healthScore >= 40 ? 'text-yellow-500' : 'text-red-500'
                }`}>
                  {route.healthScore.toFixed(0)}
                </span>
              </div>
              <div>
                <span className="font-medium">Average AQI:</span>
                <span className={`ml-2 font-bold ${
                  route.aqi <= 50 ? 'text-green-500' :
                  route.aqi <= 100 ? 'text-yellow-500' : 'text-red-500'
                }`}>
                  {route.aqi}
                </span>
              </div>
            </div>
          </div>

          <div>
            <h3 className="text-lg font-semibold mb-2">Route Map</h3>
            <div className="h-64 rounded-md overflow-hidden border">
              <MapContainer 
                center={center} 
                zoom={13} 
                style={{ height: '100%', width: '100%' }}
              >
                <TileLayer
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                />
                <Polyline
                  positions={route.polyline?.map(([lon, lat]) => [lat, lon]) || []}
                  color={route.color || '#3b82f6'}
                  weight={5}
                >
                  <Popup>
                    <div>
                      <strong>Health Score:</strong> {route.healthScore.toFixed(1)}<br />
                      <strong>AQI:</strong> {route.aqi}
                    </div>
                  </Popup>
                </Polyline>
              </MapContainer>
            </div>
          </div>
        </div>

        <div className="mt-6">
          <h3 className="text-lg font-semibold mb-2">Health Recommendations</h3>
          <div className="bg-blue-50 p-4 rounded-lg">
            {route.aqi > 100 && (
              <p className="text-red-600 font-medium mb-2">
                ⚠️ High pollution alert: Consider wearing a mask
              </p>
            )}
            {route.healthScore < 50 && (
              <p className="text-yellow-600 mb-2">
                ⚠️ This route has elevated health risks for sensitive individuals
              </p>
            )}
            <p>
              {route.healthScore >= 70 
                ? "This is a healthy route with low pollution exposure"
                : "Consider alternative routes or travel times for better air quality"}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RouteDetailPage;