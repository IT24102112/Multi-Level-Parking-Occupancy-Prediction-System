import os
import uvicorn
import numpy as np
import pandas as pd
import joblib
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict
import tensorflow as tf
from tensorflow.keras.models import load_model

# Initialize FastAPI app
app = FastAPI(title="Parking Prediction Service", 
              description="LSTM model server for 15-minute parking availability prediction.")

# Configuration
MODEL_PATH = "parking_lstm_model.keras"
SCALER_PATH = "parking_scaler_lstm.pkl"
LOOKBACK_WINDOW = 10  # This should match how the model was trained

# Global variables for model and scaler
model = None
scaler = None

@app.on_event("startup")
def load_assets():
    global model, scaler
    try:
        if os.path.exists(MODEL_PATH):
            model = load_model(MODEL_PATH)
            print(f"Model loaded from {MODEL_PATH}")
        else:
            print(f"Warning: Model file {MODEL_PATH} not found.")

        if os.path.exists(SCALER_PATH):
            scaler = joblib.load(SCALER_PATH)
            print(f"Scaler loaded from {SCALER_PATH}")
        else:
            print(f"Warning: Scaler file {SCALER_PATH} not found.")
    except Exception as e:
        print(f"Error loading model/scaler: {e}")

class PredictionRequest(BaseModel):
    # list of recent total occupancy values
    data: List[int] 

class PredictionResponse(BaseModel):
    # list of predicted total occupancy values for next 15, 30, 45, 60 mins
    predictions: List[int]

@app.post("/predict", response_model=PredictionResponse)
async def predict(request: PredictionRequest):
    if model is None or scaler is None:
        raise HTTPException(status_code=503, detail="AI Model not loaded")

    history = request.data
    try:
        # Ensure we have enough data points for the window
        if len(history) < LOOKBACK_WINDOW:
            # Pad with oldest value
            history = [history[0]] * (LOOKBACK_WINDOW - len(history)) + history
        
        # Take only the most recent 'LOOKBACK_WINDOW' values
        current_window = history[-LOOKBACK_WINDOW:]
        
        forecast = []
        # Predict 4 steps ahead (60 minutes)
        for _ in range(4):
            input_data = np.array(current_window).reshape(-1, 1)
            
            # Scale the input
            scaled_input = scaler.transform(input_data)
            
            # Reshape for LSTM: [batch, time_steps, features]
            lstm_input = scaled_input.reshape(1, LOOKBACK_WINDOW, 1)
            
            # Predict
            scaled_prediction = model.predict(lstm_input, verbose=0)
            
            # Inverse scale
            actual_prediction = scaler.inverse_transform(scaled_prediction)
            
            # Extract and round
            predicted_value = int(max(0, actual_prediction[0][0]))
            forecast.append(predicted_value)
            
            # Update window for next step (recursive)
            current_window = current_window[1:] + [predicted_value]
            
        return {"predictions": forecast}
        
    except Exception as e:
        print(f"Error predicting total occupancy: {e}")
        # Fallback to last known (return list of last value repeated)
        last_val = history[-1] if history else 0
        return {"predictions": [last_val] * 4}

@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": model is not None, "scaler_loaded": scaler is not None}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=5000)
