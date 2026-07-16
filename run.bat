@echo off
echo =====================================================================
echo           Starting PULSE.IQ SmartWatch Leaderboard Application
echo =====================================================================
echo.

echo [1/2] Starting Spring Boot Backend in a new window...
start "Pulse.IQ Backend (Spring Boot)" cmd /k "mvn spring-boot:run"

echo [2/2] Starting React Frontend in a new window...
start "Pulse.IQ Frontend (React + Vite)" cmd /k "cd frontend && npm run dev"

echo.
echo =====================================================================
echo  Both services launched successfully!
echo  - Backend will be available at: http://localhost:8081
echo  - Frontend will be available at: http://localhost:5173
echo  - Swagger UI will be available at: http://localhost:8081/swagger-ui.html
echo.
echo  Close the individual command prompt windows to stop the services.
echo =====================================================================
echo.
pause
