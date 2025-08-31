from flask import Flask, request, jsonify
from flask_cors import CORS
from enhanced_health_predictor import health_predictor
from health_assistant import get_health_advice, predict_health_score
import logging
from datetime import datetime
import os

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

# Load the ML model on startup
@app.before_first_request
def load_model():
    try:
        if not health_predictor.load_model():
            logger.warning("No trained model found. Using rule-based fallback.")
        else:
            logger.info("ML model loaded successfully")
    except Exception as e:
        logger.error(f"Error loading model: {e}")

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
    """Predict health score endpoint"""
    try:
        data = request.get_json()

        # Validate required fields
        if not data:
            return jsonify({
                'error': 'Missing request data',
                'timestamp': datetime.now().isoformat()
            }), 400

        # Get prediction
        if health_predictor.model is not None and 'latitude' in data and 'longitude' in data:
            score = health_predictor.predict_health_score(data)
            method = 'ml_model'
        else:
            score = predict_health_score(data)
            method = 'rule_based'

        return jsonify({
            'score': score,
            'calculation_method': method,
            'timestamp': datetime.now().isoformat()
        })

    except Exception as e:
        logger.error(f"Prediction error: {e}")
        return jsonify({
            'error': 'Prediction failed',
            'message': str(e),
            'timestamp': datetime.now().isoformat()
        }), 500

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
    load_model()

    # Get port from environment or use default
    port = int(os.getenv('ML_SERVICE_PORT', 8000))

    # Start the Flask app
    app.run(host='0.0.0.0', port=port, debug=False)