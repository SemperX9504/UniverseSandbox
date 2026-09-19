import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Set;

public class SolarSystem extends JFrame {
    private SolarSystemPanel panel;
    private Thread renderThread;
    private volatile boolean running = true;
    private boolean paused = false;
    private double speed = 1.0;
    private JSlider speedSlider;
    private JCheckBox gestureEnabledCheckbox;
    private boolean updatingSliderProgrammatically = false;
    private boolean updatingGestureCheckboxProgrammatically = false;
    // Gates gesture-originated action commands (rotate/zoom/pause/speed/cursor/click).
    // Toggled either by the checkbox here or by the "cross two fingers" gesture,
    // and kept in sync between the two over the command socket.
    private volatile boolean gestureControlEnabled = true;
    private volatile Socket currentClient;
    private static final int COMMAND_PORT = 5555;
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME_NS = 1_000_000_000L / TARGET_FPS;
    // Guards against a huge simulation jump after the window is minimized,
    // dragged, or the OS briefly stalls the process.
    private static final double MAX_FRAME_DT = 0.1;
    // Commands that actually move/change the simulation via gestures; these
    // are ignored while gesture control is disabled. Status/info commands
    // ("gesture", "gesture_enabled", "deselect") are never gated.
    private static final Set<String> GESTURE_ACTION_COMMANDS = Set.of(
            "rotate", "zoom", "pause", "speed_up", "speed_down", "set_speed",
            "cursor", "click", "cursor_off");

    public SolarSystem() {
        setTitle("Solar System - Waiting for Gesture Controller...");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        panel = new SolarSystemPanel();
        add(panel);
        add(createControls(), BorderLayout.SOUTH);
        setJMenuBar(createMenuBar());

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                running = false;
            }
        });

        setupInput();
        startCommandServer();
        startRenderLoop();
    }

    /**
     * Drives the simulation and repaints on its own thread, paced with
     * System.nanoTime() rather than javax.swing.Timer (which is limited to
     * the OS timer's ~15.6ms resolution on Windows and gives an uneven,
     * frame-rate-dependent simulation speed). Movement is scaled by the
     * real elapsed time so the simulation's pace no longer depends on how
     * many frames actually got rendered.
     */
    private void startRenderLoop() {
        renderThread = new Thread(() -> {
            long lastNanos = System.nanoTime();
            while (running) {
                long now = System.nanoTime();
                double dt = (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;
                dt = Math.min(dt, MAX_FRAME_DT);

                if (!paused) {
                    final double frameDt = dt;
                    try {
                        SwingUtilities.invokeAndWait(() -> panel.updateSimulation(frameDt * speed * 60.0));
                    } catch (Exception ignored) {
                    }
                }
                panel.setDisplaySpeed(speed);
                panel.repaint();
                Toolkit.getDefaultToolkit().sync();

                long elapsed = System.nanoTime() - now;
                long sleepNanos = FRAME_TIME_NS - elapsed;
                if (sleepNanos > 0) {
                    try {
                        Thread.sleep(sleepNanos / 1_000_000, (int) (sleepNanos % 1_000_000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, "RenderLoop");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu controlsMenu = new JMenu("Controls");
        controlsMenu.setFont(UiFonts.bold(13));

        addMenuHeader(controlsMenu, "Two-Hand Gestures");
        addMenuInfo(controlsMenu, "Both Palms Open + Move", "Rotate the view");
        addMenuInfo(controlsMenu, "Both Hands Pinch + Spread/Close", "Zoom in/out");
        controlsMenu.addSeparator();

        addMenuHeader(controlsMenu, "Single-Hand Gestures");
        addMenuInfo(controlsMenu, "Fist", "Toggle pause/play");
        addMenuInfo(controlsMenu, "Point Index Finger", "Cursor mode (hover planets)");
        addMenuInfo(controlsMenu, "Blink while Pointing", "Select planet under cursor");
        addMenuInfo(controlsMenu, "Thumbs Up", "Speed up");
        addMenuInfo(controlsMenu, "Thumbs Down", "Slow down");
        controlsMenu.addSeparator();

        addMenuHeader(controlsMenu, "Enable / Disable");
        addMenuInfo(controlsMenu, "\"Gesture Control\" checkbox below", "Enable / disable gesture control");
        controlsMenu.addSeparator();

        addMenuHeader(controlsMenu, "Keyboard");
        addMenuInfo(controlsMenu, "Spacebar", "Play / Pause");
        addMenuInfo(controlsMenu, "R", "Reset simulation");
        addMenuInfo(controlsMenu, "O", "Toggle orbits");
        addMenuInfo(controlsMenu, "L", "Toggle labels");
        addMenuInfo(controlsMenu, "Up / Down Arrow", "Speed up / down");
        controlsMenu.addSeparator();

        addMenuHeader(controlsMenu, "Mouse");
        addMenuInfo(controlsMenu, "Left-drag", "Rotate view");
        addMenuInfo(controlsMenu, "Right-drag / Wheel", "Zoom");

        menuBar.add(controlsMenu);
        return menuBar;
    }

    private void addMenuHeader(JMenu menu, String text) {
        JMenuItem header = new JMenuItem(text);
        header.setEnabled(false);
        header.setFont(UiFonts.bold(12));
        menu.add(header);
    }

    private void addMenuInfo(JMenu menu, String gesture, String action) {
        JMenuItem item = new JMenuItem(gesture + "   →   " + action);
        item.setEnabled(false);
        item.setFont(UiFonts.plain(12));
        menu.add(item);
    }

    private JPanel createControls() {
        JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        controls.setBackground(new Color(30, 30, 30));
        controls.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton playPause = new FlatButton("Pause", new Color(70, 130, 180));
        playPause.setPreferredSize(new Dimension(100, 35));
        playPause.addActionListener(e -> {
            paused = !paused;
            playPause.setText(paused ? "Play" : "Pause");
            panel.setPaused(paused);
        });

        speedSlider = new JSlider(1, 100, 10);
        speedSlider.setPreferredSize(new Dimension(200, 35));
        speedSlider.setBackground(new Color(30, 30, 30));
        speedSlider.setForeground(Color.WHITE);
        speedSlider.addChangeListener(e -> {
            if (!updatingSliderProgrammatically) {
                speed = speedSlider.getValue() / 10.0;
            }
        });
        speedSlider.setMajorTickSpacing(20);
        speedSlider.setMinorTickSpacing(5);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);

        JButton reset = new FlatButton("Reset", new Color(200, 60, 70));
        reset.setPreferredSize(new Dimension(100, 35));
        reset.addActionListener(e -> {
            panel.resetSimulation();
            setSpeed(1.0);
        });

        JButton orbits = new FlatButton("Toggle Orbits", new Color(50, 140, 90));
        orbits.setPreferredSize(new Dimension(120, 35));
        orbits.addActionListener(e -> panel.toggleOrbits());

        JButton labels = new FlatButton("Toggle Labels", new Color(200, 130, 40));
        labels.setPreferredSize(new Dimension(120, 35));
        labels.addActionListener(e -> panel.toggleLabels());

        gestureEnabledCheckbox = new JCheckBox("Gesture Control", true);
        gestureEnabledCheckbox.setForeground(Color.WHITE);
        gestureEnabledCheckbox.setBackground(new Color(30, 30, 30));
        gestureEnabledCheckbox.setFont(UiFonts.plain(12));
        gestureEnabledCheckbox.setFocusPainted(false);
        gestureEnabledCheckbox.addItemListener(e -> {
            if (updatingGestureCheckboxProgrammatically) return;
            gestureControlEnabled = gestureEnabledCheckbox.isSelected();
            sendToClient("{\"cmd\":\"set_gesture_enabled\",\"value\":" + gestureControlEnabled + "}");
        });

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 0;
        controls.add(createLabel("Controls:", Color.WHITE), gbc);
        gbc.gridx = 1; controls.add(playPause, gbc);
        gbc.gridx = 2; controls.add(createLabel("Speed:", Color.WHITE), gbc);
        gbc.gridx = 3; controls.add(speedSlider, gbc);
        gbc.gridx = 4; controls.add(reset, gbc);
        gbc.gridx = 5; controls.add(orbits, gbc);
        gbc.gridx = 6; controls.add(labels, gbc);
        gbc.gridx = 7; controls.add(gestureEnabledCheckbox, gbc);
        gbc.gridx = 8; controls.add(createLabel("Mouse: Left-drag=Rotate, Right-drag=Zoom, Wheel=Zoom", Color.LIGHT_GRAY), gbc);

        return controls;
    }

    private JLabel createLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(UiFonts.bold(12));
        return label;
    }

    private void setupInput() {
        panel.setFocusable(true);

        MouseAdapter mouse = new MouseAdapter() {
            Point last;
            public void mousePressed(MouseEvent e) {
                last = e.getPoint();
                panel.requestFocusInWindow();
            }
            public void mouseDragged(MouseEvent e) {
                if (last != null) {
                    int dx = e.getX() - last.x;
                    int dy = e.getY() - last.y;
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        panel.rotateView(dx * 0.008, dy * 0.008);
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        panel.adjustZoom(-dy * 0.02);
                    }
                    last = e.getPoint();
                }
            }
            public void mouseClicked(MouseEvent e) {
                if (paused) panel.handleMouseClick(e);
            }
            public void mouseWheelMoved(MouseWheelEvent e) {
                panel.adjustZoom(e.getWheelRotation() * 0.1);
            }
        };

        panel.addMouseListener(mouse);
        panel.addMouseMotionListener(mouse);
        panel.addMouseWheelListener(mouse);

        panel.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        paused = !paused;
                        panel.setPaused(paused);
                        break;
                    case KeyEvent.VK_R:
                        panel.resetSimulation();
                        break;
                    case KeyEvent.VK_O:
                        panel.toggleOrbits();
                        break;
                    case KeyEvent.VK_L:
                        panel.toggleLabels();
                        break;
                    case KeyEvent.VK_UP:
                        setSpeed(speed + 0.1);
                        break;
                    case KeyEvent.VK_DOWN:
                        setSpeed(speed - 0.1);
                        break;
                }
            }
        });
    }

    /** Sets the simulation speed and keeps the slider in sync, without the
     *  slider's own change listener feeding back and overwriting it. */
    private void setSpeed(double newSpeed) {
        speed = Math.max(0.1, Math.min(10.0, newSpeed));
        if (speedSlider != null) {
            updatingSliderProgrammatically = true;
            speedSlider.setValue((int) Math.round(speed * 10));
            updatingSliderProgrammatically = false;
        }
    }

    // Gesture Command Server
    private void startCommandServer() {
        Thread serverThread = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(COMMAND_PORT)) {
                server.setReuseAddress(true);
                System.out.println("[GestureServer] Listening on port " + COMMAND_PORT);

                while (true) {
                    try {
                        Socket client = server.accept();
                        currentClient = client;
                        System.out.println("[GestureServer] Client connected");
                        SwingUtilities.invokeLater(() -> setTitle("Solar System - Gesture Controller Connected"));
                        // Push our current enabled state so a freshly (re)connected
                        // controller and the checkbox here start in agreement.
                        sendToClient("{\"cmd\":\"set_gesture_enabled\",\"value\":" + gestureControlEnabled + "}");

                        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            processCommand(line);
                        }
                        client.close();
                    } catch (IOException e) {
                        System.out.println("[GestureServer] Client disconnected");
                    }
                    currentClient = null;
                    SwingUtilities.invokeLater(() -> {
                        setTitle("Solar System - Waiting for Gesture Controller...");
                        panel.setGestureStatus("");
                        panel.hideVirtualCursor();
                    });
                }
            } catch (IOException e) {
                System.err.println("[GestureServer] Failed: " + e.getMessage());
            }
        }, "GestureServer");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    /** Writes a JSON line back to the currently connected gesture controller,
     *  if any. Silently does nothing when there's no client (e.g. checkbox
     *  toggled before the Python side has connected). */
    private void sendToClient(String json) {
        Socket client = currentClient;
        if (client == null || client.isClosed()) return;
        try {
            OutputStream os = client.getOutputStream();
            os.write((json + "\n").getBytes("UTF-8"));
            os.flush();
        } catch (IOException e) {
            // Client likely disconnected; the accept loop will notice and clear it.
        }
    }

    private void processCommand(String json) {
        String cmd = extractString(json, "cmd");
        if (cmd == null) return;
        if (!gestureControlEnabled && GESTURE_ACTION_COMMANDS.contains(cmd)) {
            return; // gesture control is off: ignore action commands, keep status ones
        }
        SwingUtilities.invokeLater(() -> {
            switch (cmd) {
                case "rotate":
                    panel.rotateView(extractDouble(json, "dx"), extractDouble(json, "dy"));
                    break;
                case "zoom":
                    panel.adjustZoom(extractDouble(json, "delta"));
                    break;
                case "pause":
                    paused = !paused;
                    panel.setPaused(paused);
                    break;
                case "speed_up":
                    setSpeed(speed + 0.1);
                    break;
                case "speed_down":
                    setSpeed(speed - 0.1);
                    break;
                case "set_speed":
                    setSpeed(extractDouble(json, "value"));
                    break;
                case "cursor":
                    int cx = (int)(extractDouble(json, "nx") * panel.getWidth());
                    int cy = (int)(extractDouble(json, "ny") * panel.getHeight());
                    panel.setVirtualCursor(cx, cy);
                    break;
                case "click":
                    int ax = (int)(extractDouble(json, "nx") * panel.getWidth());
                    int ay = (int)(extractDouble(json, "ny") * panel.getHeight());
                    panel.selectPlanetAt(ax, ay);
                    break;
                case "cursor_off":
                    panel.hideVirtualCursor();
                    break;
                case "gesture":
                    String name = extractString(json, "name");
                    panel.setGestureStatus(name != null ? name : "");
                    break;
                case "deselect":
                    panel.clearGestureSelection();
                    break;
                case "gesture_enabled":
                    // The Python side toggled (e.g. via the cross-fingers
                    // gesture); mirror it in the checkbox without re-sending
                    // set_gesture_enabled back and bouncing the message.
                    boolean enabled = extractBool(json, "value");
                    gestureControlEnabled = enabled;
                    if (gestureEnabledCheckbox != null) {
                        updatingGestureCheckboxProgrammatically = true;
                        gestureEnabledCheckbox.setSelected(enabled);
                        updatingGestureCheckboxProgrammatically = false;
                    }
                    break;
                default:
                    break;
            }
        });
    }

    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int i = json.indexOf(pattern);
        if (i < 0) return null;
        int start = i + pattern.length();
        int end = json.indexOf("\"", start);
        return (end > start) ? json.substring(start, end) : null;
    }

    private boolean extractBool(String json, String key) {
        String pattern = "\"" + key + "\":";
        int i = json.indexOf(pattern);
        if (i < 0) return false;
        return json.startsWith("true", i + pattern.length());
    }

    private double extractDouble(String json, String key) {
        String pattern = "\"" + key + "\":";
        int i = json.indexOf(pattern);
        if (i < 0) return 0.0;
        int start = i + pattern.length();
        StringBuilder sb = new StringBuilder();
        for (int j = start; j < json.length(); j++) {
            char c = json.charAt(j);
            if (c == ',' || c == '}' || c == ' ') break;
            sb.append(c);
        }
        try {
            return Double.parseDouble(sb.toString().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SolarSystem().setVisible(true));
    }
}
