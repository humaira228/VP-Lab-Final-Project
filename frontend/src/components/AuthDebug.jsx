import React, { useState } from 'react';
import { testAuth } from '../services/api';

const AuthDebug = () => {
  const [result, setResult] = useState('');
  const [loading, setLoading] = useState(false);

  const testAuthentication = async () => {
    setLoading(true);
    try {
      const response = await testAuth();
      setResult(`Success: ${JSON.stringify(response)}`);
    } catch (error) {
      setResult(
        `Error: ${error.response?.status} - ${error.response?.data || error.message}`
      );
    }
    setLoading(false);
  };

  return (
    <div className="p-4 bg-gray-100 rounded-lg mt-4">
      <h3 className="font-bold mb-2">Authentication Debug</h3>
      <button
        onClick={testAuthentication}
        disabled={loading}
        className="bg-blue-500 text-white px-3 py-1 rounded"
      >
        {loading ? 'Testing...' : 'Test Authentication'}
      </button>
      <div className="mt-2">
        <strong>Result:</strong> {result}
      </div>
    </div>
  );
};

export default AuthDebug;
