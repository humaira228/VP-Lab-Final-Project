// ModeSelector.jsx
import React from 'react';

const ModeSelector = ({ selectedMode, onModeChange }) => {
  const modes = [
    { id: 'drive', label: 'Drive', icon: '🚗', description: 'Fastest route by car' },
    { id: 'bike', label: 'Bike', icon: '🚴', description: 'Bicycle-friendly routes' },
    { id: 'transit', label: 'Transit', icon: '🚌', description: 'Public transportation' },
    { id: 'walk', label: 'Walk', icon: '🚶', description: 'Walking paths' }
  ];

  return (
    <div className="bg-white rounded-lg shadow-md p-2 flex space-x-1">
      {modes.map(mode => (
        <button
          key={mode.id}
          onClick={() => onModeChange(mode.id)}
          className={`flex flex-col items-center justify-center p-3 rounded-md transition-all ${
            selectedMode === mode.id
              ? 'bg-green-100 text-green-600'
              : 'text-gray-600 hover:bg-gray-100'
          }`}
          title={mode.description}
        >
          <span className="text-xl mb-1">{mode.icon}</span>
          <span className="text-xs font-medium">{mode.label}</span>
        </button>
      ))}
    </div>
  );
};

export default ModeSelector;
