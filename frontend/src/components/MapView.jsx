import { useEffect, useState } from "react";
import { MapContainer, TileLayer, Polyline, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import api from "../services/api";

// Format duration into h m or m
const formatDuration = (seconds) => {
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;

  if (hours > 0) {
    return `${hours}h ${remainingMinutes}m`;
  }
  return `${minutes}m`;
};

function MapView() {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchRecommendedRoutes = async () => {
    try {
      setLoading(true);
      setError(null);

      const params = {
        startLon: 77.5946,
        startLat: 12.9716,
        endLon: 77.7,
        endLat: 12.95,
      };

      const res = await api.get("/route/recommend", { params });

      if (res.data.success === false) {
        throw new Error(res.data.message || "Backend error");
      }

      if (!res.data.routes) {
        throw new Error("No recommended routes found.");
      }

      setRoutes(res.data.routes);
    } catch (err) {
      const errorMsg =
        err.response?.data?.message ||
        err.message ||
        "Failed to load routes";
      setError(errorMsg);
      console.error("Route fetch error:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecommendedRoutes();
  }, []);

  if (loading) return <div className="p-4">Loading recommended routes...</div>;
  if (error) return <div className="p-4 text-red-500">Error: {error}</div>;
  if (!routes.length) return <div className="p-4">No routes found</div>;

  const getCenter = () => {
    try {
      const coords = JSON.parse(routes[0].polyline);
      if (coords.length > 0 && coords[0].length >= 2) {
        return [coords[0][1], coords[0][0]]; // [lat, lon]
      }
    } catch (e) {
      console.error("Error parsing polyline:", e);
    }
    return [12.9716, 77.5946];
  };

  const center = getCenter();

  return (
    <div className="space-y-4 p-4">
      <h2 className="text-2xl font-bold">Recommended Routes</h2>

      {/* Route Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {routes.map((route, index) => (
          <div
            key={route.routeId}
            className="bg-white shadow rounded-lg p-4 border border-gray-200"
          >
            <h3 className="font-semibold text-lg">Route #{index + 1}</h3>
            <p>Distance: {(route.distance / 1000).toFixed(2)} km</p>
            <p>Duration: {formatDuration(route.duration)}</p>
            <div className="flex items-center">
              <span>Health Score: </span>
              <span
                className={`ml-2 font-bold ${
                  route.healthScore >= 70
                    ? "text-green-500"
                    : route.healthScore >= 40
                    ? "text-yellow-500"
                    : "text-red-500"
                }`}
              >
                {route.healthScore.toFixed(0)}
              </span>
            </div>
            <div className="flex items-center">
              <span>AQI: </span>
              <span
                className={`ml-2 font-bold ${
                  route.aqi <= 50
                    ? "text-green-500"
                    : route.aqi <= 100
                    ? "text-yellow-500"
                    : "text-red-500"
                }`}
              >
                {route.aqi}
              </span>
            </div>
          </div>
        ))}
      </div>

      {/* Map Display */}
      <div className="h-96 w-full rounded-lg overflow-hidden border">
        <MapContainer
          center={center}
          zoom={13}
          style={{ height: "100%", width: "100%" }}
        >
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          />

          {routes.map((route) => {
            try {
              const coordinates = JSON.parse(route.polyline).map(
                ([lon, lat]) => [lat, lon]
              );

              return (
                <Polyline
                  key={route.routeId}
                  positions={coordinates}
                  color={route.color}
                  weight={5}
                >
                  <Popup>
                    <div>
                      <strong>Route ID:</strong> {route.routeId}
                      <br />
                      <strong>Score:</strong>{" "}
                      {route.healthScore.toFixed(1)}
                      <br />
                      <strong>AQI:</strong> {route.aqi}
                    </div>
                  </Popup>
                </Polyline>
              );
            } catch (e) {
              console.error("Error rendering route:", e);
              return null;
            }
          })}
        </MapContainer>
      </div>
    </div>
  );
}

export default MapView;
