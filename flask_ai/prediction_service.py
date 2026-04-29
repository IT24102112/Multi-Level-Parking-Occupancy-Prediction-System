"""
prediction_service.py  —  Smart Car Park AI Prediction Service
==============================================================
Runs on : http://localhost:5000
Called by: Spring Boot PredictionService via WebClient

Request  (from Spring Boot):
    POST /predict
    {
        "occupancy_pct"    : float,   # current occupancy as % of capacity
        "hour"             : int,     # 0-23
        "day_of_week"      : int,     # 0=Mon, 6=Sun
        "is_weekend"       : int,     # 0 or 1
        "occ_lag_1"        : float,   # occupancy_pct 10 min ago
        "occ_lag_2"        : float,   # occupancy_pct 20 min ago
        "occ_lag_3"        : float,   # occupancy_pct 30 min ago
        "rolling_mean_30m" : float    # mean of last 3 readings
    }

Response (to Spring Boot):
    {
        "predicted_occupancy_pct" : float,  # blended prediction (0-100)
        "sgd_prediction"          : float,
        "seasonal_prediction"     : float,
        "w_sgd"                   : float,
        "w_seasonal"              : float,
        "buffer_status"           : string  # e.g. "5/10"
    }

Health : GET /health  →  {"status": "running"}
Status : GET /status  →  full model info
"""

import os
import json
import math
import logging
import numpy as np
from flask import Flask, request, jsonify
from datetime import datetime

import joblib

# ── Logging ───────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)
log = logging.getLogger(__name__)

app = Flask(__name__)

# ── Base directory (all files sit next to this script) ───────────────────────
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# ── Load ensemble config ──────────────────────────────────────────────────────
config_path = os.path.join(BASE_DIR, 'ensemble_config.json')
with open(config_path, 'r') as f:
    config = json.load(f)

W_SGD      = config['w_sgd']
W_SEASONAL = config['w_seasonal']
LOOKBACK   = config['lookback_window']
CAPACITY   = config['capacity']

log.info("Ensemble config loaded:")
log.info("  SGD weight      : %s%%", round(W_SGD * 100, 1))
log.info("  Seasonal weight : %s%%", round(W_SEASONAL * 100, 1))
log.info("  Lookback window : %s", LOOKBACK)
log.info("  Capacity        : %s", CAPACITY)




# ── Load models and scalers ───────────────────────────────────────────────────
sgd_model       = joblib.load(os.path.join(BASE_DIR, 'sgd_regressor_model.pkl'))
sgd_scaler      = joblib.load(os.path.join(BASE_DIR, 'standard_scaler.pkl'))
seasonal_model  = joblib.load(os.path.join(BASE_DIR, 'seasonal_model.pkl'))
seasonal_scaler = joblib.load(os.path.join(BASE_DIR, 'seasonal_scaler.pkl'))

log.info("✅  All models and scalers loaded successfully")

# ── Sri Lankan public holidays ────────────────────────────────────────────────
HOLIDAYS = [
    '2025-10-06', '2025-11-04',
    '2025-12-04', '2025-12-25',
    '2026-01-01', '2026-04-14', '2026-04-13',
    '2026-05-01', '2026-12-25'
]

def is_holiday(date_str):
    return 1 if date_str in HOLIDAYS else 0

# ── Seasonal feature buffer ───────────────────────────────────────────────────
# Stores last LOOKBACK rows of seasonal features for LSTM sequence
seasonal_buffer = []

# ── Build seasonal features from current time and occupancy_pct ───────────────
def build_seasonal_features(now, occupancy_pct):
    hour     = now.hour
    dow      = now.weekday()
    date_str = now.strftime('%Y-%m-%d')

    hour_sin   = math.sin(2 * math.pi * hour / 24)
    hour_cos   = math.cos(2 * math.pi * hour / 24)
    day_sin    = math.sin(2 * math.pi * dow  / 7)
    day_cos    = math.cos(2 * math.pi * dow  / 7)
    is_weekend = 1 if dow >= 5 else 0
    holiday    = is_holiday(date_str)
    month      = now.month

    # Order must match SEASONAL_FEATURES_SCALER used during training:
    # [hour_sin, hour_cos, day_sin, day_cos, is_weekend, is_holiday, month, occupancy_pct]
    return [hour_sin, hour_cos, day_sin, day_cos,
            is_weekend, holiday, month, occupancy_pct]

# ── Prediction endpoint ───────────────────────────────────────────────────────
@app.route('/predict', methods=['POST'])
def predict():
    try:
        data = request.get_json(silent=True)

        if data is None:
            return jsonify({'error': 'Request body must be JSON'}), 400

        # ── Validate required fields ──────────────────────────────────────────
        required = ['occupancy_pct', 'hour', 'day_of_week',
                    'is_weekend', 'occ_lag_1', 'occ_lag_2',
                    'occ_lag_3', 'rolling_mean_30m']
        missing = [f for f in required if f not in data]
        if missing:
            return jsonify({'error': f'Missing fields: {missing}'}), 400

        log.info("📥  /predict  occ_pct=%.1f%%  hour=%s  dow=%s",
                 data['occupancy_pct'], data['hour'], data['day_of_week'])

        # ── SGD prediction ────────────────────────────────────────────────────
        realtime_features = [
            data['occupancy_pct'],
            data['hour'],
            data['day_of_week'],
            data['is_weekend'],
            data['occ_lag_1'],
            data['occ_lag_2'],
            data['occ_lag_3'],
            data['rolling_mean_30m']
        ]

        X_sgd    = sgd_scaler.transform([realtime_features])
        pred_sgd = float(sgd_model.predict(X_sgd)[0])

        # ── Seasonal prediction ───────────────────────────────────────────────
        now           = datetime.now()
        occupancy_pct = data['occupancy_pct']
        seasonal_row  = build_seasonal_features(now, occupancy_pct)

        seasonal_buffer.append(seasonal_row)
        if len(seasonal_buffer) > LOOKBACK:
            seasonal_buffer.pop(0)

        if len(seasonal_buffer) == LOOKBACK:
            X_s_scaled = seasonal_scaler.transform(seasonal_buffer)
            X_s_7      = X_s_scaled[:, :7]          # drop occupancy_pct column
            X_s_3d     = X_s_7.reshape(1, LOOKBACK, 7)
            pred_seasonal = float(
                seasonal_model.predict(X_s_3d, verbose=0)[0][0])
        else:
            # Buffer still filling up — use SGD as fallback
            pred_seasonal = pred_sgd
            log.info("Buffer filling: %s/%s", len(seasonal_buffer), LOOKBACK)

        # ── Late-fusion blend ─────────────────────────────────────────────────
        final = (W_SGD * pred_sgd) + (W_SEASONAL * pred_seasonal)
        final = float(np.clip(final, 0, 100))

        log.info("📤  sgd=%.2f  seasonal=%.2f  blended=%.2f",
                 pred_sgd, pred_seasonal, final)

        return jsonify({
            'predicted_occupancy_pct' : round(final,         2),
            'sgd_prediction'          : round(pred_sgd,      2),
            'seasonal_prediction'     : round(pred_seasonal, 2),
            'w_sgd'                   : round(W_SGD,         4),
            'w_seasonal'              : round(W_SEASONAL,    4),
            'buffer_status'           : f"{len(seasonal_buffer)}/{LOOKBACK}"
        })

    except Exception as e:
        log.error("Prediction failed: %s", e, exc_info=True)
        return jsonify({'error': str(e)}), 500

# ── Health check ──────────────────────────────────────────────────────────────
@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status'        : 'running',
        'buffer_status' : f"{len(seasonal_buffer)}/{LOOKBACK}",
        'time'          : datetime.now().isoformat()
    })

# ── Status ────────────────────────────────────────────────────────────────────
@app.route('/status', methods=['GET'])
def status():
    return jsonify({
        'status'         : 'running',
        'w_sgd'          : W_SGD,
        'w_seasonal'     : W_SEASONAL,
        'lookback'       : LOOKBACK,
        'capacity'       : CAPACITY,
        'buffer_status'  : f"{len(seasonal_buffer)}/{LOOKBACK}",
        'time'           : datetime.now().isoformat()
    })



    

    # ── Get current weights endpoint ──────────────────────────
@app.route('/weights', methods=['GET'])
def get_weights():
    with open('ensemble_config.json', 'r') as f:
        config = json.load(f)
    return jsonify({
        'w_sgd'      : config['w_sgd'],
        'w_seasonal' : config['w_seasonal'],
        'w_sgd_pct'  : round(config['w_sgd']      * 100, 1),
        'w_seasonal_pct' : round(config['w_seasonal'] * 100, 1)
    })

# ── Update weights endpoint ───────────────────────────────
@app.route('/weights', methods=['POST'])
def update_weights():
    try:
        data      = request.json
        w_sgd     = float(data['w_sgd'])
        w_seasonal = float(data['w_seasonal'])

        # Validate weights sum to 1
        if abs(w_sgd + w_seasonal - 1.0) > 0.01:
            return jsonify({
                'error': 'Weights must sum to 1.0'
            }), 400

        # Validate range
        if not (0 <= w_sgd <= 1 and 0 <= w_seasonal <= 1):
            return jsonify({
                'error': 'Each weight must be between 0 and 1'
            }), 400

        # Load current config
        with open('ensemble_config.json', 'r') as f:
            config = json.load(f)

        # Update weights
        config['w_sgd']      = round(w_sgd,      4)
        config['w_seasonal'] = round(w_seasonal, 4)

        # Save updated config
        with open('ensemble_config.json', 'w') as f:
            json.dump(config, f, indent=4)

        # Update in-memory weights immediately
        global W_SGD, W_SEASONAL
        W_SGD      = w_sgd
        W_SEASONAL = w_seasonal

        print(f"Weights updated — SGD: {round(w_sgd*100,1)}%  "
              f"Seasonal: {round(w_seasonal*100,1)}%")

        return jsonify({
            'message'        : 'Weights updated successfully',
            'w_sgd'          : round(w_sgd,      4),
            'w_seasonal'     : round(w_seasonal, 4),
            'w_sgd_pct'      : round(w_sgd      * 100, 1),
            'w_seasonal_pct' : round(w_seasonal * 100, 1)
        })

    except Exception as e:
        return jsonify({'error': str(e)}), 500

    

# ── Entry point ───────────────────────────────────────────────────────────────
if __name__ == '__main__':
    log.info("=" * 60)
    log.info("  Smart Car Park  —  Prediction Service")
    log.info("  SGD weight      : %s%%", round(W_SGD * 100, 1))
    log.info("  Seasonal weight : %s%%", round(W_SEASONAL * 100, 1))
    log.info("  Endpoint: POST http://localhost:5000/predict")
    log.info("  Health  : GET  http://localhost:5000/health")
    log.info("=" * 60)
    app.run(host='0.0.0.0', port=5000, debug=False)
