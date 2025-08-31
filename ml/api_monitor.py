import requests
import time
import logging
import json
from datetime import datetime
from pathlib import Path

class APIMonitor:
    def __init__(self):
        self.api_status = {}
        self.setup_logging()

    def setup_logging(self):
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)

    def check_api_health(self):
        """Monitor all APIs to ensure reliability"""
        apis_to_check = [
            ("AQICN", "https://api.waqi.info/feed/geo:40.7128;-74.0060/?token=21c93d78e792c675f9daa4529cfddb2faab0977a"),
            ("TomTom", "https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json?point=40.7128,-74.0060&key=l4HsFmKVhcn1ptQV6Gw1xG6bkBlU1V4h"),
            ("OpenRouteService", "https://api.openrouteservice.org/v2/directions/driving-car")
        ]

        for api_name, api_url in apis_to_check:
            try:
                start_time = time.time()
                response = requests.get(api_url, timeout=5)
                response_time = time.time() - start_time

                # Check if response contains valid data
                is_healthy = response.status_code == 200

                self.api_status[api_name] = {
                    "status": "healthy" if is_healthy else "unhealthy",
                    "response_time": round(response_time, 3),
                    "last_checked": datetime.now().isoformat(),
                    "status_code": response.status_code
                }

                status_msg = "healthy" if is_healthy else "unhealthy"
                self.logger.info(f"API {api_name} is {status_msg}, response time: {response_time:.3f}s")

            except Exception as e:
                self.api_status[api_name] = {
                    "status": "error",
                    "error": str(e),
                    "last_checked": datetime.now().isoformat()
                }
                self.logger.error(f"API {api_name} check failed: {e}")

        # Save status to file for reference
        status_path = Path(__file__).parent.parent / 'status' / 'api_status.json'
        status_path.parent.mkdir(exist_ok=True)

        with open(status_path, 'w') as f:
            json.dump(self.api_status, f, indent=2)

        return self.api_status

    def get_recommendation(self):
        """Get recommendations based on API status"""
        healthy_apis = [name for name, status in self.api_status.items()
                        if status.get('status') == 'healthy']

        if len(healthy_apis) >= 2:
            return "System status: Good - Multiple APIs available"
        elif len(healthy_apis) == 1:
            return "System status: Fair - Limited API availability"
        else:
            return "System status: Poor - No APIs available, using fallback mode"

# Example usage
if __name__ == "__main__":
    monitor = APIMonitor()
    status = monitor.check_api_health()
    print("API Status:", status)
    print(monitor.get_recommendation())