import React, { useEffect, useMemo, useRef, useState } from 'react';

const debounce = (fn, delay = 300) => {
  let t;
  return (...args) => {
    clearTimeout(t);
    t = setTimeout(() => fn(...args), delay);
  };
};

/**
 * Lightweight search box using OpenStreetMap Nominatim (no API key required)
 * Props:
 *  - label: string
 *  - placeholder?: string
 *  - onSelect: ({ lat, lon, label }) => void
 *  - value?: { lat, lon, label }
 */
export default function PlaceSearchInput({ label, placeholder, onSelect, value }) {
  const [q, setQ] = useState(value?.label || "");
  const [suggestions, setSuggestions] = useState([]);
  const [open, setOpen] = useState(false);
  const containerRef = useRef(null);

  useEffect(() => {
    setQ(value?.label || "");
  }, [value]);

  const doSearch = useMemo(
    () => debounce(async (term) => {
      if (!term || term.length < 3) {
        setSuggestions([]);
        return;
      }
      try {
        const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(term)}&limit=5&addressdetails=1`;
        const res = await fetch(url, {
          headers: {
            'Accept-Language': 'en',
          },
        });
        const data = await res.json();
        const mapped = data.map((d) => ({
          lat: parseFloat(d.lat),
          lon: parseFloat(d.lon),
          label: d.display_name,
        }));
        setSuggestions(mapped);
        setOpen(true);
      } catch (e) {
        console.error('Geocoding error', e);
      }
    }, 350),
    []
  );

  useEffect(() => {
    const onClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    document.addEventListener('click', onClickOutside);
    return () => document.removeEventListener('click', onClickOutside);
  }, []);

  return (
    <div className="relative" ref={containerRef}>
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      <input
        value={q}
        onChange={(e) => {
          const val = e.target.value;
          setQ(val);
          doSearch(val);
        }}
        placeholder={placeholder || 'Search place'}
        className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-green-500"
      />
      {open && suggestions.length > 0 && (
        <div className="absolute z-20 mt-1 w-full bg-white border rounded shadow max-h-60 overflow-auto">
          {suggestions.map((s, i) => (
            <button
              key={`${s.lat}-${s.lon}-${i}`}
              type="button"
              className="w-full text-left px-3 py-2 hover:bg-gray-100"
              onClick={() => {
                onSelect(s);
                setQ(s.label);
                setOpen(false);
              }}
            >
              <div className="text-sm font-medium">{s.label}</div>
              <div className="text-xs text-gray-500">{s.lat.toFixed(5)}, {s.lon.toFixed(5)}</div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}