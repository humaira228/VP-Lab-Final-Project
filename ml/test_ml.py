#!/usr/bin/env python3
"""
Script to test the health prediction model.
"""

import sys
import json
from pathlib import Path

# Add the ml directory to the path so we can import our modules
sys.path.insert(0, str(Path(__file__).parent))

from health_assistant import predict_health_score, get_health_advice
from enhanced_health_predictor import EnhancedHealthPredictor, HealthModelTrainer

def test_health_score():
    """Test health score prediction"""
    print("Testing health score prediction...")

    # Test data
    test_data = {
        'aqi': 120,
        'distance_km': 8.0,
        'duration_min': 45,
        'age': 35,
        'has_respiratory_issues': 1,
        'sensitivity_level': 3
    }

    # Get prediction
    score = predict_health_score(test_data)
    print(f"Health score: {score:.2f}")

    return score

def test_health_advice():
    """Test health advice generation"""
    print("\nTesting health advice generation...")

    # Test data
    user_profile = {
        'age': 35,
        'health_conditions': ['asthma'],
        'sensitivity_level': 3
    }

    route_data = {
        'distance_km': 8.0,
        'duration_min': 45
    }

    realtime_aqi = {
        'aqi': 120,
        'level': 'unhealthy_sensitive'
    }

    # Get advice
    advice = get_health_advice(user_profile, route_data, realtime_aqi)

    print("Health advice:")
    for i, item in enumerate(advice, 1):
        print(f"{i}. {item['message']} ({item['priority']} priority)")

    return advice

def test_enhanced_model():
    """Test the enhanced health predictor model"""
    print("\nTesting enhanced health predictor...")

    # Create an instance of the enhanced predictor
    predictor = EnhancedHealthPredictor()

    # Try to load the model
    model_loaded = predictor.load_model()

    if model_loaded:
        print("✓ Enhanced model loaded successfully")

        # Test prediction
        test_features = {
            'latitude': 40.7128,
            'longitude': -74.0060,
            'user_age': 35,
            'has_respiratory_issues': 1,
            'sensitivity_level': 3,
            'route_distance': 8500,
            'route_duration': 45
        }

        prediction = predictor.predict_health_score(test_features)
        print(f"✓ Enhanced model prediction: {prediction:.2f}")
    else:
        print("✗ Enhanced model not found")
        prediction = None

    return prediction

def test_model_trainer():
    """Test the model trainer"""
    print("\nTesting model trainer...")

    trainer = HealthModelTrainer()

    # Try to load existing model
    if trainer.load_model():
        print("✓ Model loaded successfully")

        # Test prediction
        test_features = {
            'latitude': 40.7128,
            'longitude': -74.0060,
            'user_age': 35,
            'has_respiratory_issues': 1,
            'sensitivity_level': 3,
            'route_distance': 8500,
            'route_duration': 45
        }

        prediction = trainer.predict_health_score(test_features)
        print(f"✓ Trainer prediction: {prediction:.2f}")
    else:
        print("✗ No model found, would need to train first")
        prediction = None

    return prediction

if __name__ == "__main__":
    print("Running ML component tests...")
    print("=" * 50)

    # Test health score prediction
    score = test_health_score()

    # Test health advice
    advice = test_health_advice()

    # Test enhanced model
    enhanced_prediction = test_enhanced_model()

    # Test model trainer
    trainer_prediction = test_model_trainer()

    print("\n" + "=" * 50)
    print("Test summary:")
    print(f"Health score prediction: {'✓' if score is not None else '✗'}")
    print(f"Health advice generation: {'✓' if advice else '✗'}")
    print(f"Enhanced model prediction: {'✓' if enhanced_prediction is not None else '✗'}")
    print(f"Model trainer prediction: {'✓' if trainer_prediction is not None else '✗'}")