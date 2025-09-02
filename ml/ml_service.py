from flask import Flask, request, jsonify
from flask_cors import CORS
from enhanced_health_predictor import health_predictor
from health_assistant import get_health_advice, predict_health_score
import logging
from datetime import datetime
import os
import json

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'timestamp': datetime.now().isoformat(),
        'model_loaded': health_predictor.model is not None
    })

@app.route('/predict', methods=['POST'])
def predict():
    """Predict health score endpoint - enhanced for route scoring"""
    try:
        data = request.get_json()
        logger.info(f"Received prediction request: {json.dumps(data, indent=2)}")

        # Validate required fields
        if not data:
            return jsonify({
                'error': 'Missing request data',
                'timestamp': datetime.now().isoformat()
            }), 400

        # Extract route information
        route_distance = data.get('route_distance', 0)
        route_duration = data.get('route_duration', 0)
        aqi = data.get('aqi', 50)
        polyline = data.get('polyline', '')

        # Extract user profile information
        user_age = data.get('user_age', 30)
        has_respiratory_issues = data.get('has_respiratory_issues', False)
        has_cardio_issues = data.get('has_cardio_issues', False)
        is_pregnant = data.get('is_pregnant', False)
        has_allergies = data.get('has_allergies', False)
        sensitivity_level = data.get('sensitivity_level', 2)
        preferred_max_aqi = data.get('preferred_max_aqi', 100)
        prefer_green_routes = data.get('prefer_green_routes', True)

        # Create feature set for prediction
        features = {
            'route_distance': route_distance,
            'route_duration': route_duration,
            'aqi': aqi,
            'user_age': user_age,
            'has_respiratory_issues': has_respiratory_issues,
            'has_cardio_issues': has_cardio_issues,
            'is_pregnant': is_pregnant,
            'has_allergies': has_allergies,
            'sensitivity_level': sensitivity_level,
            'preferred_max_aqi': preferred_max_aqi,
            'prefer_green_routes': prefer_green_routes
        }

        # Get prediction from ML model if available, otherwise use rule-based
        if health_predictor.model is not None:
            score = health_predictor.predict_health_score(features)
            method = 'ml_model'
        else:
            score = predict_health_score(features)
            method = 'rule_based'

        # Generate additional insights
        recommendations = generate_route_recommendations(score, features)

        return jsonify({
            'score': float(score),
            'calculation_method': method,
            'recommendations': recommendations,
            'timestamp': datetime.now().isoformat()
        })

    except Exception as e:
        logger.error(f"Prediction error: {e}")
        return jsonify({
            'error': 'Prediction failed',
            'message': str(e),
            'timestamp': datetime.now().isoformat()
        }), 500

def generate_route_recommendations(score, features):
    """Generate route-specific recommendations based on health score"""
    recommendations = []

    if score < 40:
        recommendations.append("This route has poor health conditions. Consider alternative routes or transportation.")
    elif score < 60:
        recommendations.append("Moderate health conditions. Sensitive individuals should consider precautions.")

    if features.get('aqi', 0) > 100:
        recommendations.append("High air pollution detected. Consider wearing a mask.")

    if features.get('has_respiratory_issues', False) and features.get('aqi', 0) > 50:
        recommendations.append("As someone with respiratory issues, you should avoid prolonged exposure to this route.")

    if features.get('prefer_green_routes', True) and features.get('aqi', 0) < 50:
        recommendations.append("This route has good air quality and is suitable for green route preferences.")

    return recommendations

@app.route('/advice', methods=['POST'])
def advice():
    """Get health advice endpoint"""
    try:
        data = request.get_json()

        # Validate required fields
        if not data:
            return jsonify({
                'error': 'Missing request data',
                'timestamp': datetime.now().isoformat()
            }), 400

        user_profile = data.get('user_profile', {})
        route_data = data.get('route_data', {})
        realtime_aqi = data.get('realtime_aqi', {})

        # Get health advice
        advice = get_health_advice(user_profile, route_data, realtime_aqi)

        return jsonify({
            'advice': advice,
            'timestamp': datetime.now().isoformat()
        })

    except Exception as e:
        logger.error(f"Advice generation error: {e}")
        return jsonify({
            'error': 'Advice generation failed',
            'message': str(e),
            'timestamp': datetime.now().isoformat()
        }), 500

if __name__ == '__main__':
    # Load model before starting
    try:
        if not health_predictor.load_model():
            logger.warning("No trained model found. Using rule-based fallback.")
        else:
            logger.info("ML model loaded successfully")
    except Exception as e:
        logger.error(f"Error loading model: {e}")

    # Get port from environment or use default
    port = int(os.getenv('ML_SERVICE_PORT', 8000))

    # Start the Flask app
    app.run(host='0.0.0.0', port=port, debug=False)