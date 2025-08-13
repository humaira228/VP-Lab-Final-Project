// HeatmapLayer.jsx
import { useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet.heat';

function HeatmapLayer({ data, type }) {
  const map = useMap();
  
  useEffect(() => {
    if (!data) return;
    
    const points = data.map(item => [item.lat, item.lon, item.intensity]);
    const heatLayer = L.heatLayer(points, {
      radius: 25,
      blur: 15,
      maxZoom: 17,
      gradient: type === 'aqi' 
        ? {0.1: '#00E400', 0.5: '#FFFF00', 0.7: '#FF7E00', 1: '#FF0000'}
        : {0.1: '#4DAF4A', 0.5: '#FFC107', 1: '#E53935'}
    }).addTo(map);
    
    return () => map.removeLayer(heatLayer);
  }, [data, map, type]);
  
  return null;
}

// Usage in MapView
<MapContainer>
  <TileLayer />
  <HeatmapLayer data={aqiData} type="aqi" />
  <HeatmapLayer data={outbreakData} type="outbreak" />
</MapContainer>