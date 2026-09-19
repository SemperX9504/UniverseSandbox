#!/bin/bash
# -------------------------------------------------------------------
#  Solar System Simulation Launcher
#  gesture_controller.py handles compiling and starting the Java app
#  internally, so this script only needs to launch the Python side.
# -------------------------------------------------------------------

# Change to the directory where this script resides
cd "$(dirname "$0")" || exit 1

# ---------------------------------------------------------------
#  If a local venv/ exists, use it; otherwise fall back to the
#  system python3 (see README.md for how to set either one up).
#  gesture_controller.py will compile US/*.java into build/, launch
#  the Java simulation (java -cp build SolarSystem), connect over
#  TCP, then open the webcam window.
# ---------------------------------------------------------------
echo "Starting gesture controller (this will also compile & launch Java)..."

if [ -f "venv/bin/activate" ]; then
    source venv/bin/activate
    python gesture_controller.py
else
    python3 gesture_controller.py
fi

echo
echo "Session ended."
read -p "Press Enter to exit..."
