#!/usr/bin/env python3
import sys
import json
import numpy as np
import joblib
import os
import traceback
from datetime import datetime

class HealthAssistant:
    def __init__(self):
        self.model = None
        self.scaler = None
        self.health_conditions_db = self._load_health_conditions()
        self.load_model()

    def _load_health_conditions(self):
        """Load health conditions and their sensitivity factors"""
        return {
            'asthma': {'aqi_sensitivity': 2.0, 'pollution_risk': 'high'},
            'copd': {'aqi_sensitivity': 2.5, 'pollution_risk': 'very_high'},
            'heart_disease': {'aqi_sensitivity': 1.8, 'pollution_risk': 'high'},
            'pregnancy': {'aqi_sensitivity': 1.5, 'pollution_risk': 'moderate'},
            'diabetes': {'aqi_sensitivity': 1.3, 'pollution_risk': 'moderate'},
            'hypertension': {'aqi_sensitivity': 1.4, 'pollution_risk': 'moderate'},
            'allergies': {'aqi_sensitivity': 1.7, 'pollution_risk': 'high'},
            'none': {'aqi_sensitivity': 1.0, 'pollution_risk': 'low'}
        }

    def load_model(self):
        """Try to load a trained model, or use rule-based system"""
        try:
            model_path = os.path.join(os.path.dirname(__file__), 'health_model.pkl')
            scaler_path = os.path.join(os.path.dirname(__file__), 'feature_scaler.pkl')

            if os.path.exists(model_path) and os.path.exists(scaler_path):
                self.model = joblib.load(model_path)
                self.scaler = joblib.load(scaler_path)
                print("Trained model loaded successfully", file=sys.stderr)
            else:
                print("No trained model found. Using rule-based health assistant.", file=sys.stderr)

        except Exception as e:
            print(f"Error loading model: {e}. Using rule-based system.", file=sys.stderr)

    def calculate_health_score(self, input_data):
        """
        Calculate health score based on multiple factors
        Uses either ML model or rule-based system
        """
        try:
            # Extract parameters
            aqi = float(input_data.get('aqi', 50))
            distance_km = float(input_data.get('distance_km', 5))
            duration_min = float(input_data.get('duration_min', 30))
            age = float(input_data.get('age', 30))
            has_respiratory_issues = bool(input_data.get('has_respiratory_issues', False))
            sensitivity_level = int(input_data.get('sensitivity_level', 2))
            prefer_green_routes = bool(input_data.get('prefer_green_routes', True))

            # Use ML model if available
            if self.model is not None and self.scaler is not None:
                features = np.array([[
                    aqi, distance_km, duration_min, age,
                    float(has_respiratory_issues), sensitivity_level, float(prefer_green_routes)
                ]])

                features_scaled = self.scaler.transform(features)
                prediction = self.model.predict(features_scaled)[0]
                return max(0, min(100, prediction))

            # Otherwise use rule-based calculation
            return self._rule_based_score(
                aqi, distance_km, duration_min, age,
                has_respiratory_issues, sensitivity_level, prefer_green_routes
            )

        except Exception as e:
            print(f"Error in health score calculation: {e}", file=sys.stderr)
            return 50.0  # Default safe score

    def _rule_based_score(self, aqi, distance_km, duration_min, age,
                          has_respiratory_issues, sensitivity_level, prefer_green_routes):
        """Rule-based health score calculation"""
        # Base score
        base_score = 100.0

        # AQI impact (0-500 scale)
        aqi_impact = max(0, (aqi - 50) * 0.15)
        base_score -= aqi_impact

        # Distance impact (shorter is better)
        distance_impact = min(15, max(0, distance_km - 2) * 0.5)
        base_score -= distance_impact

        # Duration impact (shorter is better)
        duration_impact = min(15, max(0, duration_min - 15) * 0.3)
        base_score -= duration_impact

        # Age factor
        if age < 12 or age > 65:
            base_score *= 0.9

        # Health conditions factor
        if has_respiratory_issues:
            base_score *= 0.85

        # Sensitivity level
        sensitivity_multipliers = {1: 1.0, 2: 0.9, 3: 0.8, 4: 0.7}
        base_score *= sensitivity_multipliers.get(sensitivity_level, 1.0)

        # Green routes preference
        if prefer_green_routes and aqi < 50:
            base_score *= 1.1

        return max(0, min(100, base_score))

    def get_health_advice(self, user_profile, route_data, realtime_aqi):
        """
        Provide personalized health advice based on user profile and conditions
        """
        advice = []
        conditions = user_profile.get('health_conditions', [])
        age = user_profile.get('age', 30)
        sensitivity_level = user_profile.get('sensitivity_level', 2)

        # Calculate overall risk factor
        risk_factor = self._calculate_risk_factor(conditions, age, sensitivity_level)

        # AQI-based advice
        aqi = realtime_aqi.get('aqi', 50)
        aqi_level = self._get_aqi_level(aqi)

        if aqi_level in ['unhealthy', 'very_unhealthy', 'hazardous']:
            advice.append({
                'type': 'warning',
                'message': f'Current AQI is {aqi} ({aqi_level}). Consider limiting outdoor exposure.',
                'priority': 'high'
            })

        # Condition-specific advice
        for condition in conditions:
            if condition in self.health_conditions_db:
                condition_info = self.health_conditions_db[condition]
                if condition_info['pollution_risk'] in ['high', 'very_high'] and aqi > 100:
                    advice.append({
                        'type': 'health',
                        'message': f'As someone with {condition}, you should avoid areas with high pollution today.',
                        'priority': 'high'
                    })

        # Age-based advice
        if age < 12 or age > 65:
            if aqi > 100:
                advice.append({
                    'type': 'health',
                    'message': 'People in your age group are more sensitive to air pollution. Consider staying indoors.',
                    'priority': 'medium'
                })

        # Route-specific advice
        if route_data:
            distance = route_data.get('distance_km', 0)
            duration = route_data.get('duration_min', 0)

            if distance > 5 and aqi > 100:
                advice.append({
                    'type': 'route',
                    'message': 'This is a longer route through areas with elevated pollution. Consider breaking it into shorter segments.',
                    'priority': 'medium'
                })

        # General health tips
        if aqi > 100:
            advice.extend([
                {
                    'type': 'tip',
                    'message': 'Consider wearing a mask if you need to go outside.',
                    'priority': 'low'
                },
                {
                    'type': 'tip',
                    'message': 'Keep windows closed and use air purifiers if available.',
                    'priority': 'low'
                }
            ])

        # Sort advice by priority
        priority_order = {'high': 0, 'medium': 1, 'low': 2}
        advice.sort(key=lambda x: priority_order[x['priority']])

        return advice

    def _calculate_risk_factor(self, conditions, age, sensitivity_level):
        """Calculate overall health risk factor"""
        base_risk = 1.0

        # Age factor
        if age < 12 or age > 65:
            base_risk *= 1.5

        # Conditions factor
        for condition in conditions:
            if condition in self.health_conditions_db:
                base_risk *= self.health_conditions_db[condition]['aqi_sensitivity']

        # Sensitivity level factor
        sensitivity_multipliers = {1: 1.0, 2: 1.2, 3: 1.5, 4: 2.0}
        base_risk *= sensitivity_multipliers.get(sensitivity_level, 1.0)

        return min(base_risk, 5.0)  # Cap at 5.0

    def _get_aqi_level(self, aqi):
        """Convert AQI value to level description"""
        if aqi <= 50:
            return 'good'
        elif aqi <= 100:
            return 'moderate'
        elif aqi <= 150:
            return 'unhealthy_sensitive'
        elif aqi <= 200:
            return 'unhealthy'
        elif aqi <= 300:
            return 'very_unhealthy'
        else:
            return 'hazardous'

    def get_pollution_alerts(self, location):
        """Get pollution alerts for a location"""
        return {
            'location': location,
            'alerts': [
                {
                    'type': 'pm2.5',
                    'level': 'elevated',
                    'message': 'PM2.5 levels are above recommended limits',
                    'recommendation': 'Limit outdoor activities'
                }
            ],
            'updated': datetime.now().isoformat()
        }

# Global instance
health_assistant = HealthAssistant()

def validate_input_data(data):
    """Validate input data and provide defaults for missing values"""
    validated = {}

    # Define expected fields with defaults and valid ranges
    fields = {
        'aqi': {'default': 50, 'min': 0, 'max': 500},
        'distance_km': {'default': 5, 'min': 0.1, 'max': 100},
        'duration_min': {'default': 30, 'min': 1, 'max': 300},
        'age': {'default': 30, 'min': 1, 'max': 100},
        'has_respiratory_issues': {'default': 0, 'min': 0, 'max': 1},
        'sensitivity_level': {'default': 2, 'min': 1, 'max': 4},
        'prefer_green_routes': {'default': 1, 'min': 0, 'max': 1}
    }

    for field, constraints in fields.items():
        value = data.get(field, constraints['default'])
        try:
            # Convert to appropriate type
            if field in ['has_respiratory_issues', 'prefer_green_routes']:
                value = 1 if str(value).lower() in ['true', '1', 'yes'] else 0
            else:
                value = float(value)

            # Apply constraints
            value = max(constraints['min'], min(constraints['max'], value))
            validated[field] = value
        except (ValueError, TypeError):
            validated[field] = constraints['default']
            print(f"Warning: Invalid value for {field}, using default", file=sys.stderr)

    return validated

if __name__ == "__main__":
    try:
        # Read input from STDIN (sent by Java Spring)
        raw_input = sys.stdin.read().strip()

        if not raw_input:
            raise ValueError("Empty input received")

        data = json.loads(raw_input)

        # Determine request type
        request_type = data.get('request_type', 'health_score')

        # Validate and clean input data
        validated_data = validate_input_data(data)

        response = {}

        if request_type == 'health_score':
            # Calculate health score
            try:
                score = health_assistant.calculate_health_score(validated_data)
                response = {
                    "success": True,
                    "score": float(score),
                    "model_used": "health_assistant",
                    "input_data": validated_data
                }
            except Exception as e:
                print(f"Health assistant prediction failed: {e}", file=sys.stderr)
                # Use rule-based fallback directly
                score = health_assistant._rule_based_score(
                    float(validated_data.get('aqi', 50)),
                    float(validated_data.get('distance_km', 5)),
                    float(validated_data.get('duration_min', 30)),
                    float(validated_data.get('age', 30)),
                    bool(validated_data.get('has_respiratory_issues', False)),
                    int(validated_data.get('sensitivity_level', 2)),
                    bool(validated_data.get('prefer_green_routes', True))
                )
                response = {
                    "success": True,
                    "score": float(score),
                    "model_used": "rule_based_fallback",
                    "input_data": validated_data
                }

        elif request_type == 'health_advice':
            # Get health advice
            user_profile = data.get('user_profile', {})
            route_data = data.get('route_data', {})
            realtime_aqi = data.get('realtime_aqi', {})

            try:
                advice = health_assistant.get_health_advice(user_profile, route_data, realtime_aqi)
                response = {
                    "success": True,
                    "advice": advice,
                    "request_type": "health_advice"
                }
            except Exception as e:
                response = {
                    "success": False,
                    "error": f"Failed to get health advice: {e}",
                    "request_type": "health_advice"
                }

        elif request_type == 'pollution_alerts':
            # Get pollution alerts
            location = data.get('location', {})

            try:
                alerts = health_assistant.get_pollution_alerts(location)
                response = {
                    "success": True,
                    "alerts": alerts,
                    "request_type": "pollution_alerts"
                }
            except Exception as e:
                response = {
                    "success": False,
                    "error": f"Failed to get pollution alerts: {e}",
                    "request_type": "pollution_alerts"
                }

        else:
            response = {
                "success": False,
                "error": f"Unknown request type: {request_type}",
                "request_type": request_type
            }

        # Output JSON response to STDOUT
        print(json.dumps(response))

    except json.JSONDecodeError as e:
        error_response = {
            "success": False,
            "error": f"Invalid JSON input: {str(e)}",
            "received_input": raw_input[:200] + "..." if len(raw_input) > 200 else raw_input
        }
        print(json.dumps(error_response), file=sys.stderr)
        sys.exit(1)

    except Exception as e:
        error_response = {
            "success": False,
            "error": f"Unexpected error: {str(e)}",
            "traceback": traceback.format_exc()
        }
        print(json.dumps(error_response), file=sys.stderr)
        sys.exit(1)