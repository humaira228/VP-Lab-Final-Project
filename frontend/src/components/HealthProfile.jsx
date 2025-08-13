// HealthProfile.jsx
import { useState, useEffect } from 'react';
import api from '../services/api';

function HealthProfile() {
  const [sensitivity, setSensitivity] = useState(1.0);
  const [conditions, setConditions] = useState([]);
  
  useEffect(() => {
    // Load existing profile
    api.get('/api/profile')
      .then(res => {
        setSensitivity(res.data.pollutionSensitivity || 1.0);
        setConditions(res.data.healthConditions || []);
      });
  }, []);

  const saveProfile = () => {
    api.post('/api/profile', {
      pollutionSensitivity: sensitivity,
      healthConditions: conditions
    }).then(() => alert('Profile saved!'));
  };

  return (
    <div className="max-w-md mx-auto p-6 bg-white rounded-lg shadow">
      <h2 className="text-2xl font-bold mb-6">Health Profile</h2>
      
      <div className="mb-4">
        <label className="block mb-2">
          Pollution Sensitivity: {sensitivity.toFixed(1)}x
        </label>
        <input 
          type="range" 
          min="0.5" 
          max="2.5" 
          step="0.1"
          value={sensitivity}
          onChange={e => setSensitivity(parseFloat(e.target.value))}
          className="w-full"
        />
        <div className="flex justify-between text-sm text-gray-500">
          <span>Low</span>
          <span>High</span>
        </div>
      </div>
      
      <div className="mb-6">
        <label className="block mb-2">Health Conditions:</label>
        {['Asthma', 'Allergies', 'Heart Condition', 'Respiratory Issues'].map(condition => (
          <label key={condition} className="flex items-center mb-2">
            <input
              type="checkbox"
              checked={conditions.includes(condition)}
              onChange={() => setConditions(prev => 
                prev.includes(condition) 
                  ? prev.filter(c => c !== condition) 
                  : [...prev, condition]
              )}
              className="mr-2"
            />
            {condition}
          </label>
        ))}
      </div>
      
      <button 
        onClick={saveProfile}
        className="w-full bg-green-600 text-white py-2 rounded hover:bg-green-700"
      >
        Save Profile
      </button>
    </div>
  );
}