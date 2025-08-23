import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProfile, saveProfile } from '../services/api';
import AuthDebug from '../components/AuthDebug'; // ✅ use AuthDebug instead

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
      navigate('/');
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

  if (loading) {
    return <div className="p-4">Loading profile...</div>;
  }

  return (
    <div className="p-4 max-w-lg mx-auto">
      <h1 className="text-2xl font-bold mb-4">Edit Profile</h1>

      {error && (
        <div className="mb-4 p-2 bg-red-200 text-red-800 rounded">{error}</div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="age" className="block font-medium mb-1">
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
            className="w-full border rounded p-2"
            min={0}
          />
        </div>

        <div>
          <label
            htmlFor="sensitivityLevel"
            className="block font-medium mb-1"
          >
            Sensitivity Level
          </label>
          <select
            id="sensitivityLevel"
            value={profile.sensitivityLevel}
            onChange={(e) =>
              setProfile({ ...profile, sensitivityLevel: e.target.value })
            }
            className="w-full border rounded p-2"
          >
            <option value="LOW">Low</option>
            <option value="MODERATE">Moderate</option>
            <option value="HIGH">High</option>
            <option value="VERY_HIGH">Very High</option>
          </select>
        </div>

        <div>
          <label className="block font-medium mb-1">Health Conditions</label>
          <input
            type="text"
            placeholder="Comma separated values"
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
            className="w-full border rounded p-2"
          />
        </div>

        <div className="space-y-2">
          <label className="block font-medium mb-1">
            Additional Health Info
          </label>

          <label className="flex items-center space-x-2">
            <input
              type="checkbox"
              checked={profile.hasRespiratoryIssues}
              onChange={(e) =>
                setProfile({
                  ...profile,
                  hasRespiratoryIssues: e.target.checked,
                })
              }
            />
            <span>Has Respiratory Issues</span>
          </label>

          <label className="flex items-center space-x-2">
            <input
              type="checkbox"
              checked={profile.hasCardiovascularIssues}
              onChange={(e) =>
                setProfile({
                  ...profile,
                  hasCardiovascularIssues: e.target.checked,
                })
              }
            />
            <span>Has Cardiovascular Issues</span>
          </label>

          <label className="flex items-center space-x-2">
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
            />
            <span>Is Pregnant</span>
          </label>

          <label className="flex items-center space-x-2">
            <input
              type="checkbox"
              checked={profile.hasAllergies}
              onChange={(e) =>
                setProfile({ ...profile, hasAllergies: e.target.checked })
              }
            />
            <span>Has Allergies</span>
          </label>
        </div>

        <div>
          <label
            htmlFor="preferredMaxAqi"
            className="block font-medium mb-1"
          >
            Preferred Max AQI
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
            className="w-full border rounded p-2"
            min={0}
            max={500}
          />
        </div>

        <div className="space-y-2">
          <label className="block font-medium mb-1">Preferences</label>

          <label className="flex items-center space-x-2">
            <input
              type="checkbox"
              checked={profile.avoidOutbreakZones}
              onChange={(e) =>
                setProfile({ ...profile, avoidOutbreakZones: e.target.checked })
              }
            />
            <span>Avoid Outbreak Zones</span>
          </label>

          <label className="flex items-center space-x-2">
            <input
              type="checkbox"
              checked={profile.preferGreenRoutes}
              onChange={(e) =>
                setProfile({ ...profile, preferGreenRoutes: e.target.checked })
              }
            />
            <span>Prefer Green Routes</span>
          </label>
        </div>

        <button
          type="submit"
          disabled={saving}
          className={`w-full p-3 font-bold rounded text-white ${
            saving
              ? 'bg-gray-500 cursor-not-allowed'
              : 'bg-blue-600 hover:bg-blue-700'
          }`}
        >
          {saving ? 'Saving...' : 'Save Profile'}
        </button>
      </form>

      {/* ✅ Keep AuthDebug here instead */}
      <AuthDebug />
    </div>
  );
};

export default ProfilePage;
