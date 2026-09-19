# Solar System Simulation – Gesture Controlled

## Prerequisites
- **Java JDK** (v23) must be on your `PATH` (`java` and `javac`).
- **Python 3** with `opencv-python`, `mediapipe`, and `numpy` (see the Linux
  steps below for setting these up in a venv; on Windows, install them into
  whatever `python`/`pip` is on your `PATH`).
- A **webcam** for hand/face tracking.
- The first run will download the MediaPipe model files (`hand_landmarker.task` and
  `face_landmarker.task`) into the same directory as `gesture_controller.py`, if they're
  not already there.

## How to run

### Windows
1. **Open a Command Prompt** and navigate to the Project directory:

2. **Run the launcher**:
   ```
   launch.bat
   ```
   - `gesture_controller.py` compiles the Java sources into `US\build\` (if needed),
     starts the Java simulation, connects to it over a local TCP socket, then opens
     the webcam window.
   - Close the webcam window (or press ESC) to stop; the Java simulation is closed
     automatically with it.

### Linux
There's no bundled `venv/` in this repo - set up a Python environment with
the required packages first, then run the launcher.

1. **Create a virtual environment** (recommended, from the project directory):
   ```
   python3 -m venv venv
   source venv/bin/activate
   pip install opencv-python mediapipe numpy
   ```
   You can skip the venv and `pip install` these directly into your system
   Python instead, if you prefer - `launch.sh` works either way (see below).
2. **Run the launcher**:
   ```
   ./launch.sh
   ```
   - If `venv/bin/activate` exists (from step 1), `launch.sh` activates it
     automatically. Otherwise it falls back to the system `python3`, so make
     sure the packages above are installed there if you went that route.
   - Like on Windows, this compiles the Java sources into `US/build/`,
     starts the Java simulation, connects to it over a local TCP socket,
     then opens the webcam window.
   - Close the webcam window (or press ESC) to stop; the Java simulation is
     closed automatically with it.
   - If `launch.sh` isn't executable, run `chmod +x launch.sh` once first.


## Gestures
| Gesture | Effect |
|---|---|
| Both palms open, move together | Rotate the view |
| Both hands pinch, spread/close | Zoom in/out |
| Fist | Toggle pause/play |
| Point with index finger | Cursor mode (hover over planets) |
| Blink while pointing | Select the planet under the cursor |
| Thumbs up (any hand angle) | Speed up |
| Thumbs down (any hand angle) | Slow down |

A gesture only takes effect once it's been held steady for a few frames, so a
quick, ambiguous hand pose won't misfire a different gesture. The full list
(with what each one does) is also shown any time from the **Controls**
dropdown menu at the top of the simulation window.

## Enabling/disabling gesture control
Gesture control is turned on and off with the **"Gesture Control"** checkbox
in the simulation window's control bar (there's no gesture for this on
purpose - it's checkbox-only). While disabled, mouse, keyboard, and the
on-screen buttons/slider still work as normal.

## The gesture (webcam) window
The webcam is pinned to the bottom-right corner of the screen.
Just your hand skeleton and a thin coloured border: green = active,
dim grey = gesture control disabled, red = not connected to the simulation.

## Notes
- The gesture controller sends compact JSON (`{"cmd":"rotate","dx":…}`) which the
  Java app parses directly, and Java writes status (like the enabled/disabled
  state) back over the same connection.
- Zoom sensitivity has been increased (pinch distance × 40). You can edit
  `gesture_controller.py` to adjust the multiplier if needed.
- The cursor movement is not mirrored; it follows your actual finger tip position.
- The simulation runs on its own timing thread targeting 60 FPS and is not tied to
  the OS timer resolution; simulation speed no longer depends on how fast frames
  are actually drawn.

Enjoy exploring the solar system with hand gestures!