import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SolarSystem extends JFrame {
    private SolarSystemPanel solarPanel;
    private Timer animationTimer;
    private boolean isPaused = false;
    private double speedMultiplier = 1.0;
    
    public SolarSystem() {
        setTitle("Solar System Simulation - Physics Based");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setResizable(true);
        
        solarPanel = new SolarSystemPanel();
        add(solarPanel, BorderLayout.CENTER);
        
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
        
        animationTimer = new Timer(16, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isPaused) {
                    solarPanel.updateSimulation(speedMultiplier);
                    solarPanel.repaint();
                }
            }
        });
        animationTimer.start();
        
        addMouseListeners();
        addKeyboardListeners();
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        panel.setBackground(new Color(30, 30, 30));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton playPauseBtn = new JButton(isPaused ? "Play" : "Pause");
        playPauseBtn.setPreferredSize(new Dimension(100, 35));
        playPauseBtn.setBackground(new Color(70, 130, 180));
        playPauseBtn.setForeground(Color.BLACK);
        playPauseBtn.setFocusPainted(false);
        playPauseBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isPaused = !isPaused;
                playPauseBtn.setText(isPaused ? "Play" : "Pause");
                solarPanel.setPaused(isPaused);
            }
        });
        
        JSlider speedSlider = new JSlider(1, 100, 10);
        speedSlider.setPreferredSize(new Dimension(200, 35));
        speedSlider.setBackground(new Color(30, 30, 30));
        speedSlider.setForeground(Color.WHITE);
        speedSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                speedMultiplier = speedSlider.getValue() / 10.0;
            }
        });
        speedSlider.setMajorTickSpacing(20);
        speedSlider.setMinorTickSpacing(5);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        
        JButton resetBtn = new JButton("Reset");
        resetBtn.setPreferredSize(new Dimension(100, 35));
        resetBtn.setBackground(new Color(220, 20, 60));
        resetBtn.setForeground(Color.BLACK);
        resetBtn.setFocusPainted(false);
        resetBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                solarPanel.resetSimulation();
                speedMultiplier = 1.0;
                speedSlider.setValue(10);
            }
        });
        
        JButton orbitsBtn = new JButton("Toggle Orbits");
        orbitsBtn.setPreferredSize(new Dimension(120, 35));
        orbitsBtn.setBackground(new Color(34, 139, 34));
        orbitsBtn.setForeground(Color.BLACK);
        orbitsBtn.setFocusPainted(false);
        orbitsBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                solarPanel.toggleOrbits();
            }
        });
        
        JButton labelsBtn = new JButton("Toggle Labels");
        labelsBtn.setPreferredSize(new Dimension(120, 35));
        labelsBtn.setBackground(new Color(255, 140, 0));
        labelsBtn.setForeground(Color.BLACK);
        labelsBtn.setFocusPainted(false);
        labelsBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                solarPanel.toggleLabels();
            }
        });
        
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createLabel("Controls:", Color.WHITE), gbc);
        gbc.gridx = 1; panel.add(playPauseBtn, gbc);
        gbc.gridx = 2; panel.add(createLabel("Speed:", Color.WHITE), gbc);
        gbc.gridx = 3; panel.add(speedSlider, gbc);
        gbc.gridx = 4; panel.add(resetBtn, gbc);
        gbc.gridx = 5; panel.add(orbitsBtn, gbc);
        gbc.gridx = 6; panel.add(labelsBtn, gbc);
        gbc.gridx = 7; panel.add(createLabel("Mouse: Left-drag=Rotate, Right-drag=Zoom, Wheel=Zoom", Color.LIGHT_GRAY), gbc);
        
        return panel;
    }
    
    private JLabel createLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        return label;
    }
    
    private void addMouseListeners() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            private Point lastPoint;
            
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
                solarPanel.requestFocusInWindow();
            }
            
            public void mouseDragged(MouseEvent e) {
                if (lastPoint != null) {
                    int dx = e.getX() - lastPoint.x;
                    int dy = e.getY() - lastPoint.y;
                    
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        solarPanel.rotateView(dx * 0.008, dy * 0.008);
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        solarPanel.adjustZoom(-dy * 0.02);
                    }
                    lastPoint = e.getPoint();
                }
            }
            
            public void mouseClicked(MouseEvent e) {
                if (isPaused) {
                    solarPanel.handleMouseClick(e);
                }
            }
            
            public void mouseWheelMoved(MouseWheelEvent e) {
                solarPanel.adjustZoom(e.getWheelRotation() * 0.1);
            }
        };
        
        solarPanel.addMouseListener(mouseHandler);
        solarPanel.addMouseMotionListener(mouseHandler);
        solarPanel.addMouseWheelListener(mouseHandler);
    }
    
    private void addKeyboardListeners() {
        solarPanel.setFocusable(true);
        solarPanel.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        isPaused = !isPaused;
                        solarPanel.setPaused(isPaused);
                        break;
                    case KeyEvent.VK_R:
                        solarPanel.resetSimulation();
                        break;
                    case KeyEvent.VK_O:
                        solarPanel.toggleOrbits();
                        break;
                    case KeyEvent.VK_L:
                        solarPanel.toggleLabels();
                        break;
                    case KeyEvent.VK_UP:
                        speedMultiplier = Math.min(10.0, speedMultiplier + 0.1);
                        break;
                    case KeyEvent.VK_DOWN:
                        speedMultiplier = Math.max(0.1, speedMultiplier - 0.1);
                        break;
                }
            }
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new SolarSystem().setVisible(true);
            }
        });
    }
}
