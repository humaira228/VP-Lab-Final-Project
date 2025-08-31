import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor, StackingRegressor
from sklearn.preprocessing import StandardScaler, RobustScaler
from sklearn.impute import KNNImputer
from sklearn.pipeline import Pipeline
from sklearn.model_selection import TimeSeriesSplit, cross_val_score
from sklearn.metrics import mean_squared_error, r2_score, mean_absolute_error
import xgboost as xgb
import lightgbm as lgb
import joblib
import os
from pathlib import Path
from datetime import datetime, timedelta
import logging
import requests
import json
from typing import Dict, List, Optional

# Configure logging
logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class EnhancedHealthPredictor:
    def __init__(self, config: Optional[Dict] = None):
        self.model = None
        self.scaler = None
        self.imputer = None
        self.feature_names = []
        self.config = config or {}
        self.api_cache = {}

    def load_config_from_file(self, config_path: Optional[str] = None) -> Dict:
        """Load configuration from file"""
        if config_path is None:
            config_path = Path(__file__).parent.parent / "config" / "application.properties"

        config = {}
        try:
            with open(config_path, 'r') as f:
                for line in f:
                    if '=' in line and not line.strip().startswith('#'):
                        key, value = line.strip().split('=', 1)
                        config[key] = value
        except Exception as e:
            logger.warning(f"Could not load config: {e}. Using defaults")

        return config

    def fetch_real_time_data(self, lat: float, lon: float) -> Dict:
        """Fetch real-time environmental data from APIs"""
        cache_key = f"{lat:.4f}_{lon:.4f}"
        current_time = datetime.now().timestamp()

        # Check cache first
        if cache_key in self.api_cache:
            cached_data, timestamp = self.api_cache[cache_key]
            cache_duration = 30 * 60  # 30 minutes
            if current_time - timestamp < cache_duration:
                logger.info(f"Using cached data for {lat},{lon}")
                return cached_data

        try:
            # Get API keys from config or environment
            aqicn_token = self.config.get('aqicn.token', os.getenv('AQICN_TOKEN', '21c93d78e792c675f9daa4529cfddb2faab0977a'))
            tomtom_key = self.config.get('tomtom.api.key', os.getenv('TOMTOM_API_KEY', 'l4HsFmKVhcn1ptQV6Gw1xG6bkBlU1V4h'))

            # Fetch AQI data
            aqi_url = f"https://api.waqi.info/feed/geo:{lat};{lon}/?token={aqicn_token}"
            aqi_response = requests.get(aqi_url, timeout=10)
            aqi_data = aqi_response.json()

            # Fetch traffic data
            traffic_url = f"https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json?point={lat},{lon}&key={tomtom_key}"
            traffic_response = requests.get(traffic_url, timeout=10)
            traffic_data = traffic_response.json()

            # Process and combine data
            combined_data = self.process_api_responses(aqi_data, traffic_data)
            combined_data['latitude'] = lat
            combined_data['longitude'] = lon
            combined_data['timestamp'] = current_time

            # Cache the data
            self.api_cache[cache_key] = (combined_data, current_time)

            return combined_data

        except Exception as e:
            logger.error(f"Error fetching real-time data: {e}")
            return self.generate_fallback_data(lat, lon)

    def generate_fallback_data(self, lat: float, lon: float) -> Dict:
        """Generate fallback data when APIs are unavailable"""
        # Simple fallback based on time of day and location
        hour = datetime.now().hour
        is_rush_hour = (hour >= 7 and hour <= 9) or (hour >= 16 and hour <= 19)

        return {
            'aqi': 75 if is_rush_hour else 45,
            'pm25': 35 if is_rush_hour else 15,
            'pm10': 45 if is_rush_hour else 20,
            'traffic_level': 0.8 if is_rush_hour else 0.4,
            'source': 'fallback',
            'timestamp': datetime.now().timestamp()
        }

    def process_api_responses(self, aqi_data: Dict, traffic_data: Dict) -> Dict:
        """Process API responses into features"""
        processed = {}

        # Process AQI data
        if aqi_data.get('status') == 'ok':
            data = aqi_data['data']
            processed['aqi'] = data.get('aqi', 50)

            # Process individual pollutants
            iaqi = data.get('iaqi', {})
            for poll, values in iaqi.items():
                if 'v' in values:
                    processed[poll] = values['v']

        # Process traffic data
        if 'flowSegmentData' in traffic_data:
            segment = traffic_data['flowSegmentData']
            current_speed = segment.get('currentSpeed', 0)
            free_flow_speed = segment.get('freeFlowSpeed', 1)

            processed['traffic_level'] = 1.0 - (current_speed / free_flow_speed)
            processed['current_speed'] = current_speed
            processed['free_flow_speed'] = free_flow_speed
            processed['confidence'] = segment.get('confidence', 0)

        return processed

    def create_advanced_model(self) -> Pipeline:
        """Create an advanced ensemble model"""
        # Base models
        rf_model = RandomForestRegressor(
            n_estimators=200,
            max_depth=20,
            min_samples_split=2,
            min_samples_leaf=1,
            random_state=42,
            n_jobs=-1
        )

        xgb_model = xgb.XGBRegressor(
            n_estimators=150,
            max_depth=8,
            learning_rate=0.1,
            subsample=0.8,
            colsample_bytree=0.8,
            random_state=42
        )

        lgb_model = lgb.LGBMRegressor(
            n_estimators=150,
            max_depth=7,
            learning_rate=0.05,
            subsample=0.8,
            colsample_bytree=0.8,
            random_state=42
        )

        # Create ensemble using stacking
        estimators = [
            ('random_forest', rf_model),
            ('xgboost', xgb_model),
            ('lightgbm', lgb_model)
        ]

        ensemble = StackingRegressor(
            estimators=estimators,
            final_estimator=GradientBoostingRegressor(
                n_estimators=50,
                random_state=42
            ),
            cv=5,
            n_jobs=-1
        )

        # Create pipeline with imputation and scaling
        pipeline = Pipeline([
            ('imputer', KNNImputer(n_neighbors=5)),
            ('scaler', RobustScaler()),
            ('model', ensemble)
        ])

        return pipeline

    def train_with_real_data(self, n_samples: int = 10000) -> None:
        """Train model with enhanced real-world features"""
        logger.info(f"Generating {n_samples} training samples with real-world features")

        # Generate comprehensive training data
        data = self.generate_enhanced_training_data(n_samples)

        # Prepare features and target
        X = data.drop('health_score', axis=1)
        y = data['health_score']

        # Split data using time-series cross-validation
        tscv = TimeSeriesSplit(n_splits=5)

        # Create and train model
        self.model = self.create_advanced_model()

        # Perform cross-validation
        cv_scores = cross_val_score(self.model, X, y, cv=tscv,
                                    scoring='neg_mean_squared_error', n_jobs=-1)

        logger.info(f"Cross-validation MSE: {-cv_scores.mean():.4f} (±{cv_scores.std():.4f})")

        # Train final model
        self.model.fit(X, y)

        # Make predictions for evaluation
        y_pred = self.model.predict(X)

        # Calculate comprehensive metrics
        mse = mean_squared_error(y, y_pred)
        rmse = np.sqrt(mse)
        mae = mean_absolute_error(y, y_pred)
        r2 = r2_score(y, y_pred)

        logger.info(f"Final Model Performance:")
        logger.info(f"MSE: {mse:.4f}, RMSE: {rmse:.4f}, MAE: {mae:.4f}, R²: {r2:.4f}")

        # Save model
        self.save_model()

    def generate_enhanced_training_data(self, n_samples: int) -> pd.DataFrame:
        """Generate training data with realistic environmental features"""
        np.random.seed(42)

        data = {
            # Environmental factors
            'aqi': np.random.lognormal(3.5, 0.8, n_samples).astype(int),
            'pm2_5': np.random.lognormal(2.8, 0.7, n_samples),
            'pm10': np.random.lognormal(3.0, 0.6, n_samples),
            'no2': np.random.lognormal(2.5, 0.5, n_samples),
            'o3': np.random.lognormal(2.7, 0.4, n_samples),
            'so2': np.random.lognormal(1.8, 0.5, n_samples),
            'co': np.random.lognormal(0.5, 0.3, n_samples),

            # Traffic and urban factors
            'traffic_level': np.random.beta(1.5, 3, n_samples),
            'time_of_day': np.random.randint(0, 24, n_samples),
            'day_of_week': np.random.randint(0, 7, n_samples),
            'is_weekend': np.random.choice([0, 1], n_samples, p=[0.7, 0.3]),
            'is_rush_hour': np.random.choice([0, 1], n_samples, p=[0.6, 0.4]),

            # Geographical features
            'urban_density': np.random.beta(2, 2, n_samples),
            'elevation': np.random.normal(100, 200, n_samples),
            'distance_to_road': np.random.exponential(500, n_samples),

            # User-specific factors
            'user_age': np.random.randint(18, 80, n_samples),
            'user_fitness': np.random.beta(2, 2, n_samples),
            'has_respiratory_issues': np.random.choice([0, 1], n_samples, p=[0.8, 0.2]),
            'sensitivity_level': np.random.randint(1, 5, n_samples),

            # Route characteristics
            'route_distance': np.random.lognormal(6.5, 1.0, n_samples),
            'route_duration': np.random.lognormal(7.0, 0.8, n_samples),
            'route_greenness': np.random.beta(2, 3, n_samples),
            'route_urban_ratio': np.random.beta(3, 2, n_samples)
        }

        df = pd.DataFrame(data)

        # Calculate health score based on complex interactions
        base_score = 85

        # Pollution impact (non-linear)
        pollution_impact = (
                np.log1p(df['aqi']) * 0.3 +
                np.log1p(df['pm2_5']) * 0.4 +
                np.log1p(df['no2']) * 0.2 +
                df['traffic_level'] * 20
        )

        # Positive environmental factors
        positive_impact = (
                df['route_greenness'] * 15 +
                (1 - df['urban_density']) * 10 +
                (df['elevation'] > 200) * 5  # Higher elevation often has better air
        )

        # Time-based factors
        time_impact = (
                (df['is_rush_hour'] == 1) * -10 +
                ((df['time_of_day'] >= 22) | (df['time_of_day'] <= 6)) * 5  # Night time better air
        )

        # User vulnerability factors
        user_impact = (
                (df['user_age'] < 18) * -8 +
                (df['user_age'] > 65) * -12 +
                df['has_respiratory_issues'] * -15 +
                (df['sensitivity_level'] - 1) * -6 +
                (1 - df['user_fitness']) * -10
        )

        # Route characteristics impact
        route_impact = (
                np.log1p(df['route_distance']) * -0.8 +
                np.log1p(df['route_duration']) * -1.2 +
                (df['route_urban_ratio'] > 0.7) * -10
        )

        # Calculate final score with interactions
        df['health_score'] = (
                base_score -
                pollution_impact +
                positive_impact +
                time_impact +
                user_impact +
                route_impact
        )

        # Ensure score is within bounds and add some noise
        df['health_score'] = df['health_score'].clip(0, 100)
        df['health_score'] += np.random.normal(0, 2, n_samples)
        df['health_score'] = df['health_score'].clip(0, 100)

        return df

    def predict_health_score(self, input_features: Dict) -> float:
        """Predict health score with real-time data enhancement"""
        try:
            # Enhance with real-time data if coordinates available
            if 'latitude' in input_features and 'longitude' in input_features:
                real_time_data = self.fetch_real_time_data(
                    input_features['latitude'],
                    input_features['longitude']
                )
                input_features.update(real_time_data)

            # Convert to DataFrame
            input_df = pd.DataFrame([input_features])

            # Ensure all expected features are present
            input_df = self._ensure_features(input_df)

            # Make prediction
            prediction = self.model.predict(input_df)[0]

            # Apply post-processing rules
            prediction = self._apply_post_processing(prediction, input_features)

            return max(0, min(100, prediction))

        except Exception as e:
            logger.error(f"Prediction error: {e}")
            return self._rule_based_fallback(input_features)

    def _ensure_features(self, input_df: pd.DataFrame) -> pd.DataFrame:
        """Ensure all required features are present"""
        # Define expected features based on training
        expected_features = [
            'aqi', 'pm2_5', 'pm10', 'no2', 'o3', 'so2', 'co',
            'traffic_level', 'time_of_day', 'day_of_week', 'is_weekend', 'is_rush_hour',
            'urban_density', 'elevation', 'distance_to_road',
            'user_age', 'user_fitness', 'has_respiratory_issues', 'sensitivity_level',
            'route_distance', 'route_duration', 'route_greenness', 'route_urban_ratio'
        ]

        for feature in expected_features:
            if feature not in input_df.columns:
                input_df[feature] = 0  # Default value

        return input_df[expected_features]

    def _apply_post_processing(self, prediction: float, input_features: Dict) -> float:
        """Apply business rules and post-processing"""
        # Apply minimum thresholds for extreme conditions
        if input_features.get('aqi', 0) > 200:  # Very unhealthy
            prediction = min(prediction, 30)
        elif input_features.get('aqi', 0) > 150:  # Unhealthy
            prediction = min(prediction, 50)

        # Adjust for user sensitivity
        sensitivity = input_features.get('sensitivity_level', 2)
        if sensitivity >= 4:  # Highly sensitive
            prediction *= 0.8
        elif sensitivity <= 1:  # Low sensitivity
            prediction *= 1.1

        return prediction

    def _rule_based_fallback(self, input_features: Dict) -> float:
        """Fallback rule-based prediction"""
        # Simple rule-based calculation
        aqi = input_features.get('aqi', 50)
        distance = input_features.get('route_distance', 5000) / 1000  # Convert to km
        duration = input_features.get('route_duration', 30) / 60  # Convert to hours
        traffic = input_features.get('traffic_level', 0.5)

        base_score = 80
        score = base_score - (aqi * 0.2) - (distance * 0.5) - (duration * 2) - (traffic * 10)

        return max(0, min(100, score))

    def save_model(self, model_path: Optional[str] = None) -> None:
        """Save the trained model"""
        if model_path is None:
            model_path = Path(__file__).parent / "models" / "enhanced_health_model.pkl"

        # Create directory if it doesn't exist
        os.makedirs(os.path.dirname(model_path), exist_ok=True)

        model_data = {
            'model': self.model,
            'feature_names': self.feature_names,
            'training_date': datetime.now().isoformat(),
            'version': '2.0'
        }

        joblib.dump(model_data, model_path)
        logger.info(f"Model saved to {model_path}")

    def load_model(self, model_path: Optional[str] = None) -> bool:
        """Load a trained model"""
        if model_path is None:
            model_path = Path(__file__).parent / "models" / "enhanced_health_model.pkl"

        try:
            if os.path.exists(model_path):
                model_data = joblib.load(model_path)
                self.model = model_data['model']
                self.feature_names = model_data['feature_names']
                logger.info(f"Enhanced model loaded from {model_path}")
                return True
        except Exception as e:
            logger.error(f"Error loading model: {e}")

        return False

class HealthModelTrainer:
    """Wrapper class for training health prediction models"""

    def __init__(self):
        self.predictor = EnhancedHealthPredictor()

    def train_model(self, n_samples=10000):
        """Train the health prediction model"""
        self.predictor.train_with_real_data(n_samples)
        return True

    def load_model(self, model_path=None):
        """Load a trained model"""
        return self.predictor.load_model(model_path)

    def predict_health_score(self, input_features):
        """Predict health score using the trained model"""
        return self.predictor.predict_health_score(input_features)

# Global instance for easy access
health_predictor = EnhancedHealthPredictor()

# Training execution
if __name__ == "__main__":
    # Initialize and train the enhanced model
    predictor = EnhancedHealthPredictor()

    print("Training enhanced health prediction model...")
    predictor.train_with_real_data(n_samples=15000)

    print("Model training completed!")
    print("Testing with sample data...")

    # Test with sample data
    test_sample = {
        'latitude': 40.7128,
        'longitude': -74.0060,
        'user_age': 35,
        'has_respiratory_issues': 1,
        'sensitivity_level': 3,
        'route_distance': 8500,
        'route_duration': 45
    }

    prediction = predictor.predict_health_score(test_sample)
    print(f"Sample prediction: {prediction:.2f}")