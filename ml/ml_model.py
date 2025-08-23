import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.preprocessing import StandardScaler
import joblib
import os
import json
import sys
import requests
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
                print("Trained model loaded successfully")
            else:
                print("No trained model found. Using rule-based health assistant.")

        except Exception as e:
            print(f"Error loading model: {e}. Using rule-based system.")

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
            print(f"Error in health score calculation: {e}")
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

    def get_pollution_alerts(self, location):
        """Get pollution alerts for a location"""
        # This would integrate with a real API in production
        # For now, return mock data
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

def predict_health_score(input_data):
    """Interface function for the health assistant"""
    return health_assistant.calculate_health_score(input_data)

def get_health_advice(user_profile, route_data, realtime_aqi):
    """Get personalized health advice"""
    return health_assistant.get_health_advice(user_profile, route_data, realtime_aqi)

def get_pollution_alerts(location):
    """Get pollution alerts for a location"""
    return health_assistant.get_pollution_alerts(location)

# For testing
if __name__ == "__main__":
    # Test with sample data
    test_data = {
        'aqi': 45,
        'distance_km': 3.5,
        'duration_min': 25,
        'age': 35,
        'has_respiratory_issues': 0,
        'sensitivity_level': 2,
        'prefer_green_routes': 1
    }

    score = predict_health_score(test_data)
    print(f"Health score: {score:.2f}")

    # Test health advice
    user_profile = {
        'age': 35,
        'health_conditions': ['asthma'],
        'sensitivity_level': 3
    }

    route_data = {
        'distance_km': 3.5,
        'duration_min': 25
    }

    realtime_aqi = {
        'aqi': 120,
        'level': 'unhealthy'
    }

    advice = get_health_advice(user_profile, route_data, realtime_aqi)
    print("\nHealth advice:")
    for item in advice:
        print(f"- {item['message']} ({item['priority']} priority)")