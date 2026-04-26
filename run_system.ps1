# Change directory to the root
Set-Location "c:\Users\ASUS\Downloads\Parking_management (2)"

Write-Host "--- 🐍 Setting up Python AI Prediction Service (3.12) ---" -ForegroundColor Cyan
if (-not (Test-Path "venv")) {
    python -m venv venv
}
.\venv\Scripts\pip install -r requirements.txt

Write-Host "--- 🚀 Launching AI Model Server ---" -ForegroundColor Cyan
Start-Process -NoNewWindow -FilePath ".\venv\Scripts\python.exe" -ArgumentList "predict_service.py"

Write-Host "--- ☕ Launching Spring Boot Application ---" -ForegroundColor Cyan
Set-Location "Parking_management"
# Clean and run the application
Start-Process -NoNewWindow -FilePath ".\mvnw.cmd" -ArgumentList "spring-boot:run"

Write-Host "`nSystem is starting up!" -ForegroundColor Green
Write-Host "Predict API: http://localhost:5000/health"
Write-Host "Main App:    http://localhost:8080"
Write-Host "Press Ctrl+C in their respective processes to stop them."
