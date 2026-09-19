@echo off
rem -------------------------------------------------------------------
rem  Solar System Simulation Launcher
rem  gesture_controller.py handles compiling and starting the Java app
rem  internally, so this script only needs to launch the Python side.
rem -------------------------------------------------------------------

rem Change to the directory where this script resides
cd /d "%~dp0"

rem ---------------------------------------------------------------
rem  Run the gesture controller. It compiles US/*.java into build/,
rem  launches the Java simulation (java -cp build SolarSystem),
rem  connects over TCP, then opens the webcam window.
rem ---------------------------------------------------------------
echo Starting gesture controller (this will also compile ^& launch Java)...
python gesture_controller.py
if %errorlevel% neq 0 (
    echo [!] gesture_controller.py exited with an error. See messages above.
)

echo.
echo Session ended.
pause
