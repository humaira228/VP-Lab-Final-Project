import React from 'react';

const RouteOption = ({ route, onSelect }) => {
  const getAQILevel = (aqi) => {
    if (aqi <= 50) return { text: 'Good', color: 'bg-green-500' };
    if (aqi <= 100) return { text: 'Moderate', color: 'bg-yellow-500' };
    if (aqi <= 150) return { text: 'Unhealthy for Sensitive', color: 'bg-orange-500' };
    if (aqi <= 200) return { text: 'Unhealthy', color: 'bg-red-500' };
    return { text: 'Very Unhealthy', color: 'bg-purple-500' };
  };

  const aqiInfo = getAQILevel(route.aqi);
  
  return (
    <div 
      className="border rounded-lg p-4 mb-4 cursor-pointer hover:shadow-md transition-shadow"
      onClick={() => onSelect(route)}
    >
      <div className="flex justify-between items-center mb-2">
        <h3 className="font-bold text-lg">Route Option</h3>
        <div className={`px-3 py-1 rounded-full text-white ${aqiInfo.color}`}>
          AQI: {route.aqi} ({aqiInfo.text})
        </div>
      </div>
      
      <div className="grid grid-cols-3 gap-4 mb-3">
        <div className="text-center">
          <div className="text-2xl font-bold">{(route.distance / 1000).toFixed(1)}</div>
          <div className="text-sm text-gray-500">km</div>
        </div>
        
        <div className="text-center">
          <div className="text-2xl font-bold">{Math.floor(route.duration / 60)}</div>
          <div className="text-sm text-gray-500">minutes</div>
        </div>
        
        <div className="text-center">
          <div className="text-2xl font-bold">{route.healthScore.toFixed(0)}</div>
          <div className="text-sm text-gray-500">health score</div>
        </div>
      </div>
      
      <div className="w-full bg-gray-200 rounded-full h-2.5">
        <div 
          className={`h-2.5 rounded-full ${
            route.healthScore >= 70 ? 'bg-green-500' :
            route.healthScore >= 40 ? 'bg-yellow-500' : 'bg-red-500'
          }`} 
          style={{ width: `${route.healthScore}%` }}
        ></div>
      </div>
      
      <button 
        className="mt-3 w-full bg-blue-500 text-white py-2 rounded hover:bg-blue-600"
        onClick={(e) => {
          e.stopPropagation();
          onSelect(route);
        }}
      >
        Select Route
      </button>
    </div>
  );
};