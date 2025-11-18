package Test3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SolarSystem extends JFrame {
    private SolarSystemPanel panel;
    private Timer timer;
    private boolean paused = false;
    private double speed = 1.0;
    private JButton playPauseButton;
    private JSlider speedSlider;

    public SolarSystem() {
        try {
            setTitle("Solar System Simulation - Hybrid C++ + Java");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(1400, 900);
            setLocationRelativeTo(null);

            panel = new SolarSystemPanel();
            add(panel);
            add(createControls(), BorderLayout.SOUTH);

            timer = new Timer(50, e -> {  // ~20 FPS
                if (!paused) {
                    panel.updateSimulation(speed);
                    panel.repaint();
                }
            });
            timer.start();

            setupInput();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error initializing simulation:\n" + e.getMessage(),
                    "Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createControls() {
        JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        controls.setBackground(new Color(30, 30, 30));
        controls.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        playPauseButton = new JButton("Pause");
        playPauseButton.setPreferredSize(new Dimension(100, 35));
        playPauseButton.setBackground(new Color(70, 130, 180));
        playPauseButton.setForeground(Color.BLACK);
        playPauseButton.setFocusPainted(false);
        playPauseButton.addActionListener(e -> {
            paused = !paused;
            playPauseButton.setText(paused ? "Play" : "Pause");
            panel.setPaused(paused);
        });

        speedSlider = new JSlider(1, 100, 10);
        speedSlider.setPreferredSize(new Dimension(200, 35));
        speedSlider.setBackground(new Color(30, 30, 30));
        speedSlider.setForeground(Color.WHITE);
        speedSlider.addChangeListener(e -> speed = speedSlider.getValue() / 10.0);
        speedSlider.setMajorTickSpacing(20);
        speedSlider.setMinorTickSpacing(5);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);

        JButton reset = new JButton("Reset");
        reset.setPreferredSize(new Dimension(100, 35));
        reset.setBackground(new Color(220, 20, 60));
        reset.setForeground(Color.BLACK);
        reset.addActionListener(e -> {
            panel.resetSimulation();
            speed = 1.0;
            speedSlider.setValue(10);
        });

        JButton orbits = new JButton("Toggle Orbits");
        orbits.setPreferredSize(new Dimension(120, 35));
        orbits.setBackground(new Color(34, 139, 34));
        orbits.setForeground(Color.BLACK);
        orbits.addActionListener(e -> panel.toggleOrbits());

        JButton labels = new JButton("Toggle Labels");
        labels.setPreferredSize(new Dimension(120, 35));
        labels.setBackground(new Color(255, 140, 0));
        labels.setForeground(Color.BLACK);
        labels.addActionListener(e -> panel.toggleLabels());

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 0;
        controls.add(createLabel("Controls:", Color.WHITE), gbc);
        gbc.gridx = 1; controls.add(playPauseButton, gbc);
        gbc.gridx = 2; controls.add(createLabel("Speed:", Color.WHITE), gbc);
        gbc.gridx = 3; controls.add(speedSlider, gbc);
        gbc.gridx = 4; controls.add(reset, gbc);
        gbc.gridx = 5; controls.add(orbits, gbc);
        gbc.gridx = 6; controls.add(labels, gbc);
        gbc.gridx = 7; controls.add(createLabel("Mouse: Left-drag=Rotate, Right-drag=Zoom, Wheel=Zoom", Color.LIGHT_GRAY), gbc);

        return controls;
    }

    private JLabel createLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        return label;
    }

    private void setupInput() {
        panel.setFocusable(true);
        MouseAdapter mouse = new MouseAdapter() {
            Point last;
            boolean isDragging = false;
            
            public void mousePressed(MouseEvent e) {
                last = e.getPoint();
                isDragging = false;
                panel.requestFocusInWindow();
            }
            
            public void mouseReleased(MouseEvent e) {
                // Only handle click if it wasn't a drag
                if (!isDragging && SwingUtilities.isLeftMouseButton(e)) {
                    panel.handleClick(e.getX(), e.getY());
                }
                isDragging = false;
            }
            
            public void mouseDragged(MouseEvent e) {
                isDragging = true;
                if (last != null) {
                    int dx = e.getX() - last.x;
                    int dy = e.getY() - last.y;
                    if (SwingUtilities.isLeftMouseButton(e))
                        panel.rotateView(dx * 0.008, dy * 0.008);
                    else if (SwingUtilities.isRightMouseButton(e))
                        panel.adjustZoom(-dy * 0.05);  // Adjusted for 10-increment steps
                    last = e.getPoint();
                }
            }
            public void mouseWheelMoved(MouseWheelEvent e) {
                panel.adjustZoom(e.getWheelRotation());  // Each wheel tick = ±10 zoom
            }
        };
        panel.addMouseListener(mouse);
        panel.addMouseMotionListener(mouse);
        panel.addMouseWheelListener(mouse);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SolarSystem().setVisible(true));
    }
}
