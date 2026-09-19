"""
Gesture Controller for Java Solar System Simulation
=====================================================
Launches the Java solar system app, connects via TCP socket,
and sends gesture commands from webcam hand/face tracking.

Two-Hand Gestures:
    Both palms open + move  -->  Rotate the solar system
    Both hands pinch + spread/close  -->  Zoom in/out

Single-Hand Gestures:
    Fist   -->  Toggle pause/play
    Point index finger  -->  Cursor mode (hover over planets)
    Blink while pointing  -->  Select planet (show details)
    Thumbs up  -->  Speed up
    Thumbs down  -->  Slow down

Gesture control can be enabled/disabled from the "Gesture Control" checkbox
in the simulation window (there's no gesture for this - it's checkbox-only).

Press ESC in the camera window to quit.
"""

import cv2
import mediapipe as mp
from mediapipe.tasks import python as mp_python
from mediapipe.tasks.python import vision as mp_vision
import numpy as np
import socket
import json
import math
import time
import os
import platform
import subprocess
import threading
from collections import deque

# ──────────────────────────────────────────────
#  Constants
# ──────────────────────────────────────────────
JAVA_SRC_ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__)))
COMMAND_PORT  = 5555
CAM_INDEX     = 0

# The frame captured from the camera and used for detection. Kept modest so
# tracking stays cheap; the on-screen window is smaller still (see below).
CAM_W, CAM_H = 640, 360

# The gesture window is deliberately small and tucked in a corner of the
# screen (see screenshot reference), so it carries no readable text overlay
# at all - any text drawn at this size would just look like a smear.
WIN_NAME = "Gesture Controller"
WIN_W, WIN_H = 480, 270
WIN_MARGIN_RIGHT = 4
WIN_MARGIN_BOTTOM = 88  # leaves room for the Windows/desktop taskbar

GESTURE_STABLE_FRAMES = 5      # frames a label must repeat before it "fires"
SPEED_STEP_INTERVAL   = 0.4    # seconds between speed multiplier steps
SPEED_STEP_FACTOR     = 1.5
SPEED_MIN, SPEED_MAX  = 0.1, 10.0

# Hand landmark connections for drawing
HAND_CONNS = [
    (0,1),(1,2),(2,3),(3,4),          # thumb
    (0,5),(5,6),(6,7),(7,8),          # index
    (5,9),(9,10),(10,11),(11,12),     # middle
    (9,13),(13,14),(14,15),(15,16),   # ring
    (13,17),(17,18),(18,19),(19,20),  # pinky
    (0,17),                            # palm
]


def _screen_size():
    """Best-effort primary screen size, for positioning the small preview
    window in a corner. Falls back to a common resolution if unavailable
    (e.g. a headless Linux box)."""
    try:
        import tkinter
        root = tkinter.Tk()
        root.withdraw()
        w, h = root.winfo_screenwidth(), root.winfo_screenheight()
        root.destroy()
        return w, h
    except Exception:
        return 1920, 1080


# ──────────────────────────────────────────────
#  Gesture Controller
# ──────────────────────────────────────────────
class GestureController:
    def __init__(self):
        script_dir = os.path.dirname(os.path.abspath(__file__))
        # ── MediaPipe Hand Landmarker (2 hands) ──
        hand_model = os.path.join(script_dir, "hand_landmarker.task")
        if not os.path.exists(hand_model):
            print("[!] hand_landmarker.task not found. Downloading...")
            import urllib.request
            urllib.request.urlretrieve(
                "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task",
                hand_model)
        self.hand_lm = mp_vision.HandLandmarker.create_from_options(
            mp_vision.HandLandmarkerOptions(
                base_options=mp_python.BaseOptions(model_asset_path=hand_model),
                running_mode=mp_vision.RunningMode.VIDEO,
                num_hands=2,
                min_hand_detection_confidence=0.6,
                min_hand_presence_confidence=0.6,
                min_tracking_confidence=0.5,
            ))
        # ── MediaPipe Face Landmarker (blink detection) ──
        face_model = os.path.join(script_dir, "face_landmarker.task")
        if not os.path.exists(face_model):
            print("[!] face_landmarker.task not found. Downloading...")
            import urllib.request
            urllib.request.urlretrieve(
                "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/latest/face_landmarker.task",
                face_model)
        self.face_lm = mp_vision.FaceLandmarker.create_from_options(
            mp_vision.FaceLandmarkerOptions(
                base_options=mp_python.BaseOptions(model_asset_path=face_model),
                running_mode=mp_vision.RunningMode.VIDEO,
                num_faces=1,
                min_face_detection_confidence=0.5,
                min_face_presence_confidence=0.5,
                min_tracking_confidence=0.5,
                output_face_blendshapes=True,
            ))
        # ── State ──
        self.sock: socket.socket = None
        self.connected = False
        self.hand_ts = 0
        self.face_ts = 0
        self._t0 = time.time()
        self.prev_data = {}
        self.cur_gesture = "Waiting..."
        self.cursor_mode = False
        self.cursor_nx = 0.0
        self.cursor_ny = 0.0
        self.fist_cd = 0.0
        self.blink_cd = 0.0
        self.java_proc = None
        self.blink_state = False
        self.frame_count = 0

        # Gesture stabilizer: only act once the same label repeats N frames.
        self._label_history = deque(maxlen=GESTURE_STABLE_FRAMES)
        self._active_label = "none"

        # Continuous speed-adjust state
        self.sim_speed = 1.0
        self._last_speed_step = 0.0

        # Gesture-control enable/disable state, toggled remotely by the
        # "Gesture Control" checkbox in the Java UI (kept in sync over the
        # command socket; see _socket_reader).
        self.gesture_enabled = True

    # ── Java lifecycle ────────────────────────
    def launch_java(self):
        """Compile and start the Java solar system."""
        os.makedirs(os.path.join(JAVA_SRC_ROOT, "build"), exist_ok=True)
        print("[*] Compiling Java...")
        comp = subprocess.run(
            ["javac", "-d", "build",
             "US/SolarSystem.java", "US/SolarSystemPanel.java", "US/CelestialBody.java",
             "US/UiFonts.java", "US/FlatButton.java"],
            cwd=JAVA_SRC_ROOT, capture_output=True, text=True)
        if comp.returncode != 0:
            print("[!] Compile failed:\n" + comp.stderr)
            return False
        print("[*] Compilation OK. Starting Java app...")
        # stdout/stderr are discarded rather than piped: an unread PIPE can
        # fill up and deadlock the Java process once it logs enough output.
        self.java_proc = subprocess.Popen(
            ["java", "-Dsun.java2d.uiScale.enabled=true", "-cp", "build", "SolarSystem"],
            cwd=JAVA_SRC_ROOT,
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        return True

    # ── Socket ────────────────────────────────
    def connect(self, retries=30):
        for i in range(retries):
            try:
                s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                s.connect(("localhost", COMMAND_PORT))
                self.sock = s
                self.connected = True
                print(f"[*] Connected to Java on port {COMMAND_PORT}")
                # Tell Java our current enabled state so its checkbox starts
                # in agreement with us (it also pushes its own state back).
                self.send({"cmd": "gesture_enabled", "value": self.gesture_enabled})
                threading.Thread(target=self._socket_reader, daemon=True).start()
                return True
            except ConnectionRefusedError:
                print(f"    Waiting for Java server... ({i+1}/{retries})")
                time.sleep(1)
        print("[!] Could not connect to Java.")
        return False

    def send(self, cmd: dict):
        if not self.connected:
            return
        try:
            self.sock.sendall((json.dumps(cmd, separators=(",",":")) + "\n").encode())
        except (BrokenPipeError, ConnectionResetError, OSError):
            self.connected = False
            print("[!] Connection lost.")

    def _socket_reader(self):
        """Reads status messages Java writes back on the same socket - right
        now just its checkbox's enable/disable state - so both UIs stay in
        sync no matter which side changed it."""
        buf = ""
        sock = self.sock
        while self.connected and sock:
            try:
                data = sock.recv(4096)
            except OSError:
                break
            if not data:
                break
            buf += data.decode(errors="ignore")
            while "\n" in buf:
                line, buf = buf.split("\n", 1)
                line = line.strip()
                if not line:
                    continue
                try:
                    msg = json.loads(line)
                except ValueError:
                    continue
                if msg.get("cmd") == "set_gesture_enabled":
                    self.gesture_enabled = bool(msg.get("value", True))

    # ── Detection helpers ─────────────────────
    def _now_ms(self):
        return int((time.time() - self._t0) * 1000)

    def detect_hands(self, frame):
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        img = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
        ts = self._now_ms()
        if ts <= self.hand_ts:
            ts = self.hand_ts + 1
        self.hand_ts = ts
        return self.hand_lm.detect_for_video(img, ts)

    def detect_blink(self, frame) -> bool:
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        img = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
        ts = self._now_ms()
        if ts <= self.face_ts:
            ts = self.face_ts + 1
        self.face_ts = ts
        res = self.face_lm.detect_for_video(img, ts)
        if not res.face_blendshapes:
            return False
        bs = {b.category_name: b.score for b in res.face_blendshapes[0]}
        return bs.get("eyeBlinkLeft",0) > 0.35 and bs.get("eyeBlinkRight",0) > 0.35

    # ── Gesture classification ────────────────
    @staticmethod
    def _palm_size(lm) -> float:
        # Distance from wrist (0) to middle-finger knuckle (9); a stable
        # reference length regardless of hand orientation or distance to cam.
        return math.hypot(lm[9].x - lm[0].x, lm[9].y - lm[0].y) or 1e-6

    @classmethod
    def _fingers_up(cls, lm) -> list:
        """Which of index/middle/ring/pinky are extended, independent of
        hand rotation: a finger is 'up' when its tip sits further from the
        wrist than its PIP joint does (rather than assuming the hand is
        upright, which breaks for sideways/upside-down gestures)."""
        wrist = lm[0]
        out = []
        for tip, pip in [(8,6),(12,10),(16,14),(20,18)]:
            d_tip = math.hypot(lm[tip].x - wrist.x, lm[tip].y - wrist.y)
            d_pip = math.hypot(lm[pip].x - wrist.x, lm[pip].y - wrist.y)
            out.append(d_tip > d_pip * 1.05)
        return out

    @classmethod
    def _thumb_extended(cls, lm) -> bool:
        palm = cls._palm_size(lm)
        d = math.hypot(lm[4].x - lm[5].x, lm[4].y - lm[5].y)
        return d > 0.9 * palm

    @staticmethod
    def _thumb_direction(lm):
        """Returns 'up', 'down', or None based on the thumb's angle,
        independent of overall hand rotation."""
        dx = lm[4].x - lm[2].x
        dy = lm[4].y - lm[2].y
        mag = math.hypot(dx, dy) or 1e-6
        dx, dy = dx / mag, dy / mag
        # Image y grows downward, so "up" is negative dy.
        angle_from_up = math.degrees(math.acos(max(-1.0, min(1.0, -dy))))
        if angle_from_up < 45:
            return "up"
        if angle_from_up > 135:
            return "down"
        return None

    @staticmethod
    def _is_pinch(lm) -> bool:
        return math.hypot(lm[4].x - lm[8].x, lm[4].y - lm[8].y) < 0.06

    def _classify(self, lm) -> str:
        idx, mid, ring, pinky = self._fingers_up(lm)
        if self._is_pinch(lm):
            return "pinch"
        thumb_out = self._thumb_extended(lm)
        curled = not any([idx, mid, ring, pinky])
        # Thumbs up/down must be checked before "fist": in both poses every
        # other finger is curled, which used to be misread as a fist.
        if thumb_out and curled:
            direction = self._thumb_direction(lm)
            if direction == "up":
                return "thumbs_up"
            if direction == "down":
                return "thumbs_down"
        if curled and not thumb_out:
            return "fist"
        if idx and not mid and not ring and not pinky:
            return "pointing"
        if sum([idx, mid, ring, pinky]) >= 3:
            return "open_palm"
        return "none"

    def _stabilize(self, label: str) -> str:
        """Requires a label to persist for several consecutive frames before
        it becomes 'active', so single misread frames don't cause flicker
        (e.g. pause toggling on and off, or a gesture name flashing)."""
        self._label_history.append(label)
        if len(self._label_history) == self._label_history.maxlen and \
           all(x == label for x in self._label_history):
            self._active_label = label
        return self._active_label

    # ── Gesture processing ────────────────────
    def process(self, hands_res, blinked: bool):
        now = time.time()
        lms = hands_res.hand_landmarks or []
        n = len(lms)

        if not self.gesture_enabled:
            self._exit_cursor()
            self.cur_gesture = "Disabled (use the Gesture Control checkbox to enable)"
            self.prev_data = {}
            self._label_history.clear()
            return

        if not lms:
            self._label_history.clear()
            self._active_label = "none"
            if self.cursor_mode:
                self.send({"cmd": "cursor_off"})
                self.cursor_mode = False
            self.cur_gesture = "No Hands Detected"
            self.send({"cmd": "gesture", "name": ""})
            self.prev_data = {}
            return
        if n == 2:
            g0 = self._classify(lms[0])
            g1 = self._classify(lms[1])
            if g0 == "pinch" and g1 == "pinch":
                self._label_history.clear()
                c0 = (lms[0][9].x, lms[0][9].y)
                c1 = (lms[1][9].x, lms[1][9].y)
                dist = math.hypot(c0[0]-c1[0], c0[1]-c1[1])
                if "pinch_dist" in self.prev_data:
                    delta = (dist - self.prev_data["pinch_dist"]) * 40.0
                    self.send({"cmd": "zoom", "delta": round(delta,4)})
                self.prev_data = {"pinch_dist": dist}
                self.cur_gesture = "ZOOM (both pinch)"
                self.send({"cmd": "gesture", "name": "Zoom"})
                self._exit_cursor()
                return
            if g0 == "open_palm" and g1 == "open_palm":
                self._label_history.clear()
                mx = (lms[0][9].x + lms[1][9].x) / 2
                my = (lms[0][9].y + lms[1][9].y) / 2
                if "mid" in self.prev_data:
                    dx = mx - self.prev_data["mid"][0]
                    dy = my - self.prev_data["mid"][1]
                    self.send({"cmd": "rotate", "dx": round(-dx*5.0,4), "dy": round(dy*5.0,4)})
                self.prev_data = {"mid": (mx, my)}
                self.cur_gesture = "ROTATE (both palms)"
                self.send({"cmd": "gesture", "name": "Rotate"})
                self._exit_cursor()
                return

        # Single-hand gestures use the first detected hand, stabilized so a
        # gesture only "fires" once it has been held steadily.
        raw = self._classify(lms[0])
        g = self._stabilize(raw)

        if g == "fist":
            if now - self.fist_cd > 1.5:
                self.fist_cd = now
                self.send({"cmd": "pause"})
                self.cur_gesture = "FIST -> Pause/Play"
                self.send({"cmd": "gesture", "name": "Pause / Play"})
                self._exit_cursor()
            self.prev_data = {}
            return

        if g == "thumbs_up":
            if now - self._last_speed_step > SPEED_STEP_INTERVAL:
                self._last_speed_step = now
                self.sim_speed = min(SPEED_MAX, self.sim_speed * SPEED_STEP_FACTOR)
                self.send({"cmd": "set_speed", "value": round(self.sim_speed, 3)})
            self.cur_gesture = f"THUMBS UP -> Speed Up ({self.sim_speed:.1f}x)"
            self.send({"cmd": "gesture", "name": "Speed Up"})
            self._exit_cursor()
            self.prev_data = {}
            return

        if g == "thumbs_down":
            if now - self._last_speed_step > SPEED_STEP_INTERVAL:
                self._last_speed_step = now
                self.sim_speed = max(SPEED_MIN, self.sim_speed / SPEED_STEP_FACTOR)
                self.send({"cmd": "set_speed", "value": round(self.sim_speed, 3)})
            self.cur_gesture = f"THUMBS DOWN -> Slow Down ({self.sim_speed:.1f}x)"
            self.send({"cmd": "gesture", "name": "Slow Down"})
            self._exit_cursor()
            self.prev_data = {}
            return

        if g == "pointing":
            tx = lms[0][8].x
            ty = lms[0][8].y
            self.cursor_nx = tx
            self.cursor_ny = ty
            self.cursor_mode = True
            self.send({"cmd": "cursor", "nx": round(tx,4), "ny": round(ty,4)})
            self.cur_gesture = "POINT -> Cursor"
            self.send({"cmd": "gesture", "name": "Cursor Mode"})
            if blinked and now - self.blink_cd > 1.5:
                self.blink_cd = now
                self.send({"cmd": "click", "nx": round(tx,4), "ny": round(ty,4)})
                self.cur_gesture = "BLINK -> Planet Selected!"
                self.send({"cmd": "gesture", "name": "Planet Selected!"})
            self.prev_data = {}
            return

        if n == 1 and g == "open_palm":
            self.cur_gesture = "One palm (use both to rotate)"
            self.send({"cmd": "gesture", "name": "Use both palms to rotate"})
        self.prev_data = {}

    def _exit_cursor(self):
        if self.cursor_mode:
            self.send({"cmd": "cursor_off"})
            self.cursor_mode = False

    # ── Drawing helpers ───────────────────────
    @staticmethod
    def draw_hands(frame, landmarks_list):
        h, w = frame.shape[:2]
        for lm_list in landmarks_list:
            pts = {}
            for i, lm in enumerate(lm_list):
                cx, cy = int(lm.x * w), int(lm.y * h)
                pts[i] = (cx, cy)
            for a,b in HAND_CONNS:
                if a in pts and b in pts:
                    cv2.line(frame, pts[a], pts[b], (0,220,190), 2, cv2.LINE_AA)
            for i, (cx, cy) in pts.items():
                c = (0,255,140) if i in (4,8,12,16,20) else (220,220,220)
                cv2.circle(frame, (cx,cy), 4, c, -1, cv2.LINE_AA)
                cv2.circle(frame, (cx,cy), 5, (40,40,40), 1, cv2.LINE_AA)

    def draw_status_border(self, frame):
        """The window is too small to carry readable text, so status is
        shown purely as a thin border colour: green = active, dim grey =
        gesture control disabled, red = not connected to the simulation."""
        if not self.connected:
            color = (0, 0, 220)
        elif not self.gesture_enabled:
            color = (90, 90, 90)
        elif self.blink_state:
            color = (0, 210, 255)
        else:
            color = (0, 200, 90)
        h, w = frame.shape[:2]
        cv2.rectangle(frame, (0, 0), (w - 1, h - 1), color, 3, cv2.LINE_AA)

    # ── Camera helpers ────────────────────────
    @staticmethod
    def _open_camera(index):
        system = platform.system()
        if system == "Windows":
            cap = cv2.VideoCapture(index, cv2.CAP_DSHOW)
        elif system == "Linux":
            cap = cv2.VideoCapture(index, cv2.CAP_V4L2)
        else:
            cap = cv2.VideoCapture(index)
        cap.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc(*"MJPG"))
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, CAM_W)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, CAM_H)
        cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
        return cap

    # ── Main loop ─────────────────────────────
    def run(self):
        print("="*55)
        print("  Solar System - Gesture Controller")
        print("="*55)
        if not self.launch_java():
            print("[!] Failed to start Java. Exiting.")
            return
        if not self.connect():
            if self.java_proc:
                self.java_proc.terminate()
            return
        cap = self._open_camera(CAM_INDEX)
        if not cap.isOpened():
            print("[!] Camera not available.")
            return
        print("[*] Gesture control active. Show your hands!")
        print("    Press ESC in the camera window to quit.\n")

        cv2.namedWindow(WIN_NAME, cv2.WINDOW_NORMAL)
        cv2.resizeWindow(WIN_NAME, WIN_W, WIN_H)
        screen_w, screen_h = _screen_size()
        win_x = max(0, screen_w - WIN_W - WIN_MARGIN_RIGHT)
        win_y = max(0, screen_h - WIN_H - WIN_MARGIN_BOTTOM)
        cv2.moveWindow(WIN_NAME, win_x, win_y)

        try:
            while True:
                ret, frame = cap.read()
                if not ret:
                    break
                frame = cv2.flip(frame, 1)
                if frame.shape[1] != CAM_W or frame.shape[0] != CAM_H:
                    frame = cv2.resize(frame, (CAM_W, CAM_H))
                self.frame_count += 1

                hr = self.detect_hands(frame)
                blinked = False
                if self.cursor_mode and self.frame_count % 2 == 0:
                    blinked = self.detect_blink(frame)
                    self.blink_state = blinked
                elif not self.cursor_mode:
                    self.blink_state = False
                if hr.hand_landmarks:
                    self.draw_hands(frame, hr.hand_landmarks)
                self.process(hr, blinked)

                display = frame if (CAM_W, CAM_H) == (WIN_W, WIN_H) else cv2.resize(frame, (WIN_W, WIN_H))
                self.draw_status_border(display)
                cv2.imshow(WIN_NAME, display)
                if cv2.waitKey(1) & 0xFF == 27:
                    break
                if self.java_proc and self.java_proc.poll() is not None:
                    print("[*] Java app closed.")
                    break
        except KeyboardInterrupt:
            pass
        print("\n[*] Shutting down...")
        cap.release()
        cv2.destroyAllWindows()
        self.hand_lm.close()
        self.face_lm.close()
        if self.sock:
            self.sock.close()
        if self.java_proc and self.java_proc.poll() is None:
            self.java_proc.terminate()
            print("[*] Java process terminated.")

if __name__ == "__main__":
    ctrl = GestureController()
    ctrl.run()
