import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.preprocessing import StandardScaler
import joblib
import os
from pathlib import Path
from datetime import datetime
import logging

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class HealthAssistant:
    def __init__(self):
        self.health_conditions_db = self._load_health_conditions()

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

    def predict_health_score(self, input_data):
        """Interface function for health score prediction"""
        # Extract basic parameters
        aqi = input_data.get('aqi', 50)
        distance_km = input_data.get('distance_km', 5)
        duration_min = input_data.get('duration_min', 30)
        age = input_data.get('age', 30)
        has_respiratory_issues = input_data.get('has_respiratory_issues', False)
        sensitivity_level = input_data.get('sensitivity_level', 2)

        # Simple rule-based calculation
        base_score = 80.0

        # AQI impact
        aqi_impact = max(0, (aqi - 50) * 0.2)
        base_score -= aqi_impact

        # Distance impact
        distance_impact = min(15, max(0, distance_km - 2) * 0.5)
        base_score -= distance_impact

        # Duration impact
        duration_impact = min(15, max(0, duration_min - 15) * 0.3)
        base_score -= duration_impact

        # Health conditions factor
        if has_respiratory_issues:
            base_score *= 0.85

        # Sensitivity level
        sensitivity_multipliers = {1: 1.0, 2: 0.9, 3: 0.8, 4: 0.7}
        base_score *= sensitivity_multipliers.get(sensitivity_level, 1.0)

        # Age factor
        if age < 12 or age > 65:
            base_score *= 0.9

        return max(0, min(100, base_score))

# Global instance
health_assistant = HealthAssistant()

def get_health_advice(user_profile, route_data, realtime_aqi):
    """Get personalized health advice"""
    return health_assistant.get_health_advice(user_profile, route_data, realtime_aqi)

def predict_health_score(input_data):
    """Interface function for health score prediction"""
    return health_assistant.predict_health_score(input_data)