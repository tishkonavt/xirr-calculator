@echo off
echo ========================================
echo XIRR Calculator
echo ========================================
echo.

set JAVA_HOME=C:\Users\stishkin\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.16.8-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Starting server...
echo Open browser at: http://localhost:8080
echo.
echo Press Ctrl+C to stop the server
echo.

java -jar releases\xirr-calculator-all.jar

pause
