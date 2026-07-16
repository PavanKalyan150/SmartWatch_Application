Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "          Starting PULSE.IQ SmartWatch Leaderboard Application" -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[1/2] Starting Spring Boot Backend in a new window..." -ForegroundColor Gray
Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn spring-boot:run"

Write-Host "[2/2] Starting React Frontend in a new window..." -ForegroundColor Gray
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd frontend; npm run dev"

Write-Host ""
Write-Host "=====================================================================" -ForegroundColor Green
Write-Host " Both services launched successfully!" -ForegroundColor Green
Write-Host " - Backend will be available at: http://localhost:8081" -ForegroundColor Green
Write-Host " - Frontend will be available at: http://localhost:5173" -ForegroundColor Green
Write-Host " - Swagger UI will be available at: http://localhost:8081/swagger-ui.html" -ForegroundColor Green
Write-Host ""
Write-Host " Close the individual terminal windows to stop the services." -ForegroundColor Green
Write-Host "=====================================================================" -ForegroundColor Green
Write-Host ""
