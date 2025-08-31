import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProfile, saveProfile } from '../services/api';
import AuthDebug from '../components/AuthDebug';

const ProfilePage = ({ setIsAuthenticated }) => {
  const [profile, setProfile] = useState({
    age: null,
    sensitivityLevel: 'MODERATE',
    healthConditions: [],
    hasRespiratoryIssues: false,
    hasCardiovascularIssues: false,
    isPregnant: false,
    hasAllergies: false,
    preferredMaxAqi: 100,
    avoidOutbreakZones: true,
    preferGreenRoutes: true,
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await getProfile();
        if (response) {
          setProfile({
            age: response.age || null,
            sensitivityLevel: response.sensitivityLevel || 'MODERATE',
            healthConditions: response.healthConditions || [],
            hasRespiratoryIssues: response.hasRespiratoryIssues || false,
            hasCardiovascularIssues: response.hasCardiovascularIssues || false,
            isPregnant: response.isPregnant || false,
            hasAllergies: response.hasAllergies || false,
            preferredMaxAqi: response.preferredMaxAqi || 100,
            avoidOutbreakZones: response.avoidOutbreakZones !== false,
            preferGreenRoutes: response.preferGreenRoutes !== false,
          });
        }
      } catch (err) {
        console.error('Failed to load profile', err);
        if (err.response?.status === 401 || err.response?.status === 403) {
          setIsAuthenticated(false);
        }
        setError('Failed to load profile. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [setIsAuthenticated]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);

    try {
      await saveProfile(profile);
      setIsEditing(false);
    } catch (err) {
      console.error('Failed to save profile', err);
      if (err.response?.status === 401 || err.response?.status === 403) {
        setIsAuthenticated(false);
      }
      setError(
        err.response?.data?.message ||
          'Failed to save profile. Please try again.'
      );
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    // Reset to original profile data
    const fetchOriginalData = async () => {
      try {
        const response = await getProfile();
        if (response) {
          setProfile({
            age: response.age || null,
            sensitivityLevel: response.sensitivityLevel || 'MODERATE',
            healthConditions: response.healthConditions || [],
            hasRespiratoryIssues: response.hasRespiratoryIssues || false,
            hasCardiovascularIssues: response.hasCardiovascularIssues || false,
            isPregnant: response.isPregnant || false,
            hasAllergies: response.hasAllergies || false,
            preferredMaxAqi: response.preferredMaxAqi || 100,
            avoidOutbreakZones: response.avoidOutbreakZones !== false,
            preferGreenRoutes: response.preferGreenRoutes !== false,
          });
        }
      } catch (err) {
        console.error('Failed to reset profile', err);
        setError('Failed to reset changes. Please try again.');
      }
    };
    
    fetchOriginalData();
    setIsEditing(false);
  };

  const getSensitivityColor = (level) => {
    switch(level) {
      case 'LOW': return 'bg-green-100 text-green-800';
      case 'MODERATE': return 'bg-yellow-100 text-yellow-800';
      case 'HIGH': return 'bg-orange-100 text-orange-800';
      case 'VERY_HIGH': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-2xl mx-auto bg-white rounded-xl shadow-md overflow-hidden">
        <div className="p-8">
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-3xl font-bold text-gray-800">Profile Settings</h1>
            {!isEditing && (
              <button
                onClick={() => setIsEditing(true)}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center"
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
                Edit Profile
              </button>
            )}
          </div>

          {error && (
            <div className="mb-6 p-4 bg-red-50 text-red-700 rounded-lg border border-red-200">
              {error}
            </div>
          )}

          {!isEditing ? (
            // View Mode
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="bg-gray-50 p-4 rounded-lg">
                  <h2 className="text-lg font-semibold text-gray-700 mb-4">Personal Information</h2>
                  <div className="space-y-3">
                    <div className="flex justify-between">
                      <span className="text-gray-600">Age</span>
                      <span className="font-medium">{profile.age || 'Not specified'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Sensitivity Level</span>
                      <span className={`px-3 py-1 rounded-full text-sm font-medium ${getSensitivityColor(profile.sensitivityLevel)}`}>
                        {profile.sensitivityLevel.replace('_', ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="bg-gray-50 p-4 rounded-lg">
                  <h2 className="text-lg font-semibold text-gray-700 mb-4">Health Conditions</h2>
                  <div className="space-y-3">
                    <div className="flex justify-between">
                      <span className="text-gray-600">Conditions</span>
                      <div className="text-right">
                        {profile.healthConditions.length > 0 ? (
                          profile.healthConditions.map((condition, index) => (
                            <span key={index} className="block text-sm font-medium">{condition}</span>
                          ))
                        ) : (
                          <span className="text-sm text-gray-500">None specified</span>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-gray-50 p-4 rounded-lg">
                <h2 className="text-lg font-semibold text-gray-700 mb-4">Health Status</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="flex items-center">
                    <span className={`inline-flex items-center justify-center w-8 h-8 rounded-full mr-3 ${profile.hasRespiratoryIssues ? 'bg-red-100' : 'bg-green-100'}`}>
                      <svg xmlns="http://www.w3.org/2000/svg" className={`h-4 w-4 ${profile.hasRespiratoryIssues ? 'text-red-600' : 'text-green-600'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                      </svg>
                    </span>
                    <span className="text-gray-700">Respiratory Issues: {profile.hasRespiratoryIssues ? 'Yes' : 'No'}</span>
                  </div>
                  <div className="flex items-center">
                    <span className={`inline-flex items-center justify-center w-8 h-8 rounded-full mr-3 ${profile.hasCardiovascularIssues ? 'bg-red-100' : 'bg-green-100'}`}>
                      <svg xmlns="http://www.w3.org/2000/svg" className={`h-4 w-4 ${profile.hasCardiovascularIssues ? 'text-red-600' : 'text-green-600'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                      </svg>
                    </span>
                    <span className="text-gray-700">Cardiovascular Issues: {profile.hasCardiovascularIssues ? 'Yes' : 'No'}</span>
                  </div>
                  <div className="flex items-center">
                    <span className={`inline-flex items-center justify-center w-8 h-8 rounded-full mr-3 ${profile.isPregnant ? 'bg-blue-100' : 'bg-gray-100'}`}>
                      <svg xmlns="http://www.w3.org/2000/svg" className={`h-4 w-4 ${profile.isPregnant ? 'text-blue-600' : 'text-gray-600'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                      </svg>
                    </span>
                    <span className="text-gray-700">Pregnancy: {profile.isPregnant ? 'Yes' : 'No'}</span>
                  </div>
                  <div className="flex items-center">
                    <span className={`inline-flex items-center justify-center w-8 h-8 rounded-full mr-3 ${profile.hasAllergies ? 'bg-yellow-100' : 'bg-green-100'}`}>
                      <svg xmlns="http://www.w3.org/2000/svg" className={`h-4 w-4 ${profile.hasAllergies ? 'text-yellow-600' : 'text-green-600'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                      </svg>
                    </span>
                    <span className="text-gray-700">Allergies: {profile.hasAllergies ? 'Yes' : 'No'}</span>
                  </div>
                </div>
              </div>

              <div className="bg-gray-50 p-4 rounded-lg">
                <h2 className="text-lg font-semibold text-gray-700 mb-4">Preferences</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="flex items-center">
                    <span className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-blue-100 mr-3">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                      </svg>
                    </span>
                    <span className="text-gray-700">Max AQI: {profile.preferredMaxAqi}</span>
                  </div>
                  <div className="flex items-center">
                    <span className={`inline-flex items-center justify-center w-8 h-8 rounded-full mr-3 ${profile.avoidOutbreakZones ? 'bg-green-100' : 'bg-gray-100'}`}>
                      <svg xmlns="http://www.w3.org/2000/svg" className={`h-4 w-4 ${profile.avoidOutbreakZones ? 'text-green-600' : 'text-gray-600'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                      </svg>
                    </span>
                    <span className="text-gray-700">Avoid Outbreak Zones: {profile.avoidOutbreakZones ? 'Yes' : 'No'}</span>
                  </div>
                  <div className="flex items-center">
                    <span className={`inline-flex items-center justify-center w-8 h-8 rounded-full mr-3 ${profile.preferGreenRoutes ? 'bg-green-100' : 'bg-gray-100'}`}>
                      <svg xmlns="http://www.w3.org/2000/svg" className={`h-4 w-4 ${profile.preferGreenRoutes ? 'text-green-600' : 'text-gray-600'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                    </span>
                    <span className="text-gray-700">Prefer Green Routes: {profile.preferGreenRoutes ? 'Yes' : 'No'}</span>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            // Edit Mode
            <form onSubmit={handleSubmit} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label htmlFor="age" className="block text-sm font-medium text-gray-700 mb-2">
                    Age
                  </label>
                  <input
                    type="number"
                    id="age"
                    value={profile.age ?? ''}
                    onChange={(e) =>
                      setProfile({
                        ...profile,
                        age: e.target.value ? Number(e.target.value) : null,
                      })
                    }
                    className="w-full border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    min={0}
                  />
                </div>

                <div>
                  <label htmlFor="sensitivityLevel" className="block text-sm font-medium text-gray-700 mb-2">
                    Sensitivity Level
                  </label>
                  <select
                    id="sensitivityLevel"
                    value={profile.sensitivityLevel}
                    onChange={(e) =>
                      setProfile({ ...profile, sensitivityLevel: e.target.value })
                    }
                    className="w-full border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  >
                    <option value="LOW">Low</option>
                    <option value="MODERATE">Moderate</option>
                    <option value="HIGH">High</option>
                    <option value="VERY_HIGH">Very High</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Health Conditions (comma separated)
                </label>
                <input
                  type="text"
                  placeholder="e.g. Asthma, Diabetes, Hypertension"
                  value={profile.healthConditions.join(', ')}
                  onChange={(e) =>
                    setProfile({
                      ...profile,
                      healthConditions: e.target.value
                        .split(',')
                        .map((s) => s.trim())
                        .filter(Boolean),
                    })
                  }
                  className="w-full border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>

              <div className="border-t border-gray-200 pt-6">
                <h3 className="text-lg font-medium text-gray-800 mb-4">Additional Health Information</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <label className="flex items-center space-x-3">
                    <input
                      type="checkbox"
                      checked={profile.hasRespiratoryIssues}
                      onChange={(e) =>
                        setProfile({
                          ...profile,
                          hasRespiratoryIssues: e.target.checked,
                        })
                      }
                      className="h-5 w-5 text-blue-600 rounded focus:ring-blue-500"
                    />
                    <span className="text-gray-700">Has Respiratory Issues</span>
                  </label>

                  <label className="flex items-center space-x-3">
                    <input
                      type="checkbox"
                      checked={profile.hasCardiovascularIssues}
                      onChange={(e) =>
                        setProfile({
                          ...profile,
                          hasCardiovascularIssues: e.target.checked,
                        })
                      }
                      className="h-5 w-5 text-blue-600 rounded focus:ring-blue-500"
                    />
                    <span className="text-gray-700">Has Cardiovascular Issues</span>
                  </label>

                  <label className="flex items-center space-x-3">
                    <input
                      type="checkbox"
                      checked={profile.isPregnant}
                      onChange={(e) =>
                        setProfile({
                          ...profile,
                          isPregnant: e.target.checked,
                        })
                      }
                      disabled={profile.age !== null && profile.age < 12}
                      className="h-5 w-5 text-blue-600 rounded focus:ring-blue-500"
                    />
                    <span className="text-gray-700">Is Pregnant</span>
                  </label>

                  <label className="flex items-center space-x-3">
                    <input
                      type="checkbox"
                      checked={profile.hasAllergies}
                      onChange={(e) =>
                        setProfile({ ...profile, hasAllergies: e.target.checked })
                      }
                      className="h-5 w-5 text-blue-600 rounded focus:ring-blue-500"
                    />
                    <span className="text-gray-700">Has Allergies</span>
                  </label>
                </div>
              </div>

              <div className="border-t border-gray-200 pt-6">
                <h3 className="text-lg font-medium text-gray-800 mb-4">Preferences</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <label htmlFor="preferredMaxAqi" className="block text-sm font-medium text-gray-700 mb-2">
                      Preferred Maximum AQI
                    </label>
                    <input
                      type="number"
                      id="preferredMaxAqi"
                      value={profile.preferredMaxAqi}
                      onChange={(e) =>
                        setProfile({
                          ...profile,
                          preferredMaxAqi: e.target.value
                            ? Number(e.target.value)
                            : 100,
                        })
                      }
                      className="w-full border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      min={0}
                      max={500}
                    />
                  </div>

                  <div className="space-y-4">
                    <label className="flex items-center space-x-3">
                      <input
                        type="checkbox"
                        checked={profile.avoidOutbreakZones}
                        onChange={(e) =>
                          setProfile({ ...profile, avoidOutbreakZones: e.target.checked })
                        }
                        className="h-5 w-5 text-blue-600 rounded focus:ring-blue-500"
                      />
                      <span className="text-gray-700">Avoid Outbreak Zones</span>
                    </label>

                    <label className="flex items-center space-x-3">
                      <input
                        type="checkbox"
                        checked={profile.preferGreenRoutes}
                        onChange={(e) =>
                          setProfile({ ...profile, preferGreenRoutes: e.target.checked })
                        }
                        className="h-5 w-5 text-blue-600 rounded focus:ring-blue-500"
                      />
                      <span className="text-gray-700">Prefer Green Routes</span>
                    </label>
                  </div>
                </div>
              </div>

              <div className="flex justify-end space-x-4 pt-6 border-t border-gray-200">
                <button
                  type="button"
                  onClick={handleCancel}
                  className="px-5 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className={`px-5 py-2 rounded-lg text-white font-medium transition-colors ${
                    saving
                      ? 'bg-gray-400 cursor-not-allowed'
                      : 'bg-blue-600 hover:bg-blue-700'
                  }`}
                >
                  {saving ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
      
    
    </div>
  );
};

export default ProfilePage;