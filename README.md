https://radiant-lily-55c610.netlify.app/

# RouteAI – EcoTrack 🌿

**Health-Conscious Route Planning with AI Integration**

A full-stack web application that provides personalized, health-focused route recommendations. It combines real-time air quality data, user health profiles, and machine learning to calculate a **Health Safety Score** for navigation alternatives, helping users avoid pollution and disease hotspots.

Built for **Therap JavaFest**.

---

## ✨ Features

* **Intelligent Routing**: Multiple route alternatives scored on health factors, not just distance.
* **Real-Time Environmental Data**: Live Air Quality Index (AQI) from the WAQI API.
* **Personalized Health Profiles**: Tailored advice based on health conditions (e.g., asthma, allergies).
* **Machine Learning Health Scoring**: AI-powered prediction via a Python Flask microservice.
* **Interactive Map Visualization**: React + Leaflet.js maps with routes and environmental heatmaps.
* **Secure JWT Authentication**: Full user registration & login system.
* **Robust Fallback System**: Java backend falls back to rule-based scoring if ML service is unavailable.

---

## 🏗️ System Architecture
graph TB
    A[React Frontend] --> B[Spring Boot Backend]
    B --> C[PostgreSQL DB]
    B --> D[Python ML Service]
    B --> E[WAQI API]
    B --> F[OpenRouteService API]
    D --> G[Trained ML Model]

    style A fill:#61dafb
    style B fill:#6db33f
    style D fill:#3776ab


### 🛠️ Technology Stack

**Backend (/backend)**

* Java 17
* Spring Boot 3.2 (Web, Security, Data JPA)
* Spring Security with JWT Authentication
* PostgreSQL with PostGIS extension
* Gradle Build Tool

**Frontend (/frontend)**

* React 18
* React Router (Navigation)
* Leaflet.js & React-Leaflet (Maps)
* Tailwind CSS (Styling)
* Axios (API Client)

**AI/ML Service (/ml-service)**

* Python 3.10+
* Flask (REST API)
* Scikit-learn, XGBoost, LightGBM (ML Models)
* Joblib (Model Persistence)

**APIs & Data Sources**

* OpenRouteService API (Route Geometry & Data)
* WAQI API (Real-time Air Quality Data)

---

## ⚙️ Installation & Setup

### Prerequisites

* JDK 17
* Node.js (v18 or higher) + npm
* Python 3.10+ + pip
* PostgreSQL (v12+) with PostGIS extension
* Git

---

### 1. Clone the Repository

```bash
git clone <your-repo-url>
cd ecotrack
```

---

### 2. Database Setup

```sql
CREATE DATABASE ecotrack;
CREATE EXTENSION postgis;
```

---

### 3. Backend Setup (Spring Boot)

```bash
cd backend
```

Edit `application.properties` with:

* `spring.datasource.url`, `username`, `password`
* `aqicn.token` (WAQI API key)
* `ors.api.key` (OpenRouteService API key)

Run the backend:

```bash
./gradlew bootRun
```

👉 Server runs at `http://localhost:9090`

---

### 4. Frontend Setup (React)

```bash
cd frontend
npm install
npm run dev
```

👉 Frontend runs at `http://localhost:5173`

---

### 5. ML Service Setup (Python Flask)

```bash
cd ml-service
python -m venv venv
# Windows: venv\Scripts\activate
# macOS/Linux: source venv/bin/activate

pip install -r requirements.txt
python app.py
```

👉 ML service runs at `http://localhost:5000`

---

## 🔐 Configuration

### API Keys (Required)

In `backend/src/main/resources/application.properties`:

```properties
# WAQI API for Air Quality Data
aqicn.token=your_waqi_api_key_here

# OpenRouteService API for Routing
ors.api.key=your_ors_api_key_here

# PostgreSQL Database
spring.datasource.url=jdbc:postgresql://localhost:5432/ecotrack
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
```

* **WAQI**: Sign up at [waqi.info](https://waqi.info)
* **OpenRouteService**: Sign up at [openrouteservice.org](https://openrouteservice.org)

### ML Service Connection

Spring Boot backend calls the Flask ML service (`http://localhost:5000`).
If unavailable, the **fallback rule-based algorithm** is used.

---

## 🧠 Machine Learning Model

**EnhancedHealthPredictor** (`/ml-service`):

* Uses features:

  * Real-time AQI
  * Route distance & duration
  * Historical pollution data
  * User health profile (conditions, sensitivities)
  * Time of day & weather (optional)

**Model**: Stacking Regressor with:

* Random Forest
* Gradient Boosting (XGBoost, LightGBM)

Model trained on **historical urban environmental data**, stored as `.pkl`.

---

## 🚀 Deployment

### Backend (Spring Boot)

```bash
cd backend
./gradlew build -x test
java -jar build/libs/ecotrack-0.0.1-SNAPSHOT.jar
```

### Frontend (React)

```bash
cd frontend
npm run build
```

Deploy `build` folder (Vercel, Netlify, S3).

### ML Service (Flask)

```bash
pip install gunicorn
gunicorn -w 4 -b 0.0.0.0:5000 app:app
```

Run with **PM2**:

```bash
pm2 start --name "ml-service" "gunicorn -w 4 -b 0.0.0.0:5000 app:app"
```

---

## 📊 API Endpoints

| Method | Endpoint                         | Description                      |
| ------ | -------------------------------- | -------------------------------- |
| POST   | `/api/auth/register`             | User registration                |
| POST   | `/api/auth/login`                | User login (returns JWT)         |
| GET    | `/api/route/recommend`           | Get health-scored routes         |
| GET    | `/api/aqi`                       | Get AQI for a location           |
| POST   | `/api/profile`                   | Save user health profile         |
| POST   | `http://ml-service:5000/predict` | (ML Service) Get AI health score |

---

## 🧪 Testing the System

1. Start all services (**PostgreSQL, Spring Boot, React, Flask**)
2. Open `http://localhost:5173` in browser
3. Register a new user account
4. Set up **Health Profile** in the Profile section
5. Go to **Map Page** → generate recommended routes

---

## 📄 License

This project is created for **educational and competition purposes**.
**All rights reserved.**

