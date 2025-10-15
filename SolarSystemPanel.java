import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class SolarSystemPanel extends JPanel {
    private List<CelestialBody> planets = new ArrayList<>();
    private CelestialBody sun, selectedPlanet;
    private double rotationX = 0, rotationY = 0, rotationZ = 0;
    private double zoom = 80, scale = 60, simulationTime = 0;
    private static final double TIME_STEP = 0.006;
    private boolean showOrbits = true, showLabels = true, isPaused = false;
    private Color orbitColor = new Color(0, 255, 0, 60);
    
    public SolarSystemPanel() {
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(1200, 900));
        initializeSolarSystem();
    }
    
    private void initializeSolarSystem() {
        sun = new CelestialBody("Sun", CelestialBody.SOLAR_MASS, 696340, Color.YELLOW, 0, 0, 0, 0);
        
        planets.add(new CelestialBody("Mercury", 3.301e23, 2439.7, Color.GRAY, 0.39, 0.206, 0.24, 7.0));
        planets.add(new CelestialBody("Venus", 4.867e24, 6051.8, new Color(255,180,10), 0.72, 0.007, 0.62, 3.4));
        planets.add(new CelestialBody("Earth", 5.972e24, 6371, new Color(70,130,250), 1.0, 0.017, 1.0, 0.0));
        planets.add(new CelestialBody("Mars", 6.39e23, 3389.5, new Color(255,60,30), 1.52, 0.094, 1.88, 1.9));
        planets.add(new CelestialBody("Jupiter", 1.898e27, 69911, new Color(200,170,60), 5.20, 0.049, 11.86, 1.3));
        planets.add(new CelestialBody("Saturn", 5.683e26, 58232, new Color(210,180,140), 9.58, 0.057, 29.46, 2.5));
        planets.add(new CelestialBody("Uranus", 8.681e25, 25362, new Color(85,255,255), 19.22, 0.046, 84.01, 0.8));
        planets.add(new CelestialBody("Neptune", 1.024e26, 24622, new Color(60,130,255), 30.05, 0.010, 164.8, 1.8));
    }
    
    public void updateSimulation(double speedMultiplier) {
        simulationTime += TIME_STEP * speedMultiplier;
        for (CelestialBody planet : planets) {
            planet.updatePosition(TIME_STEP, speedMultiplier);
        }
    }
    
    public void resetSimulation() {
        simulationTime = 0;
        selectedPlanet = null;
        for (CelestialBody planet : planets) {
            planet.reset();
        }
        repaint();
    }
    
    public void rotateView(double deltaX, double deltaY) {
        rotationY += deltaX;
        rotationX += deltaY;
        repaint();
    }
    
    public void adjustZoom(double delta) {
        zoom = Math.max(5, Math.min(1000, zoom + delta * 10));
        repaint();
    }
    
    public void toggleOrbits() {
        showOrbits = !showOrbits;
        repaint();
    }
    
    public void toggleLabels() {
        showLabels = !showLabels;
        repaint();
    }
    
    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }
    
    public void handleMouseClick(MouseEvent e) {
        if (!isPaused) return;
        
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int clickX = e.getX();
        int clickY = e.getY();
        
        // Check if clicking on Edit button
        if (selectedPlanet != null) {
            int panelWidth = 250;
            int panelHeight = 230;
            int panelX = getWidth() - panelWidth - 15;
            int panelY = 15;
            int buttonWidth = 100;
            int buttonHeight = 30;
            int buttonX = panelX + (panelWidth - buttonWidth) / 2;
            int buttonY = panelY + panelHeight - buttonHeight - 10;
            
            if (clickX >= buttonX && clickX <= buttonX + buttonWidth &&
                clickY >= buttonY && clickY <= buttonY + buttonHeight) {
                openEditDialog();
                return;
            }
        }
        
        selectedPlanet = null;
        
        int baseSunSize = Math.max(15, (int)(zoom / 4));
        int sunSize = (int)(baseSunSize * getSizeRatio(sun));
        if (isPointInCircle(clickX, clickY, centerX, centerY, sunSize/2)) {
            selectedPlanet = sun;
            repaint();
            return;
        }
        
        for (CelestialBody planet : planets) {
            CelestialBody.Point3D pos = planet.getRotatedPosition(rotationX, rotationY, rotationZ);
            double perspectiveScale = 1.0 / (1.0 + Math.abs(pos.z) * 0.08);
            int px = centerX + (int)(pos.x * scale * zoom / 100 * perspectiveScale);
            int py = centerY + (int)(pos.y * scale * zoom / 100 * perspectiveScale);
            
            double sizeRatio = getSizeRatio(planet);
            int planetSize = (int)(baseSunSize * sizeRatio * perspectiveScale);
            planetSize = Math.max(planetSize, getMinimumSize(planet));
            
            if (isPointInCircle(clickX, clickY, px, py, planetSize/2 + 5)) {
                selectedPlanet = planet;
                repaint();
                return;
            }
        }
        
        repaint();
    }
    
    private boolean isPointInCircle(int pointX, int pointY, int centerX, int centerY, int radius) {
        double distance = Math.sqrt(Math.pow(pointX - centerX, 2) + Math.pow(pointY - centerY, 2));
        return distance <= radius;
    }
    
    private double getSizeRatio(CelestialBody body) {
        double sunRadius = sun.getRadius();
        return body.getRadius() / sunRadius;
    }
    
    private int getMinimumSize(CelestialBody body) {
        double ratio = getSizeRatio(body);
        if (ratio > 0.08) return 6;
        if (ratio > 0.05) return 5;
        if (ratio > 0.03) return 4;
        return 3;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        drawOrbits(g2d, centerX, centerY);
        drawSun(g2d, centerX, centerY);
        drawPlanets(g2d, centerX, centerY);
        drawInfoPanel(g2d);
        
        if (selectedPlanet != null && isPaused) {
            drawPlanetAttributes(g2d);
        }
    }
    
    private void drawOrbits(Graphics2D g2d, int cx, int cy) {
        if (!showOrbits) return;
        g2d.setColor(orbitColor);
        g2d.setStroke(new BasicStroke(1.5f));
        
        for (CelestialBody planet : planets) {
            List<CelestialBody.Point3D> trail = planet.getRotatedTrail(rotationX, rotationY, rotationZ);
            
            if (trail.size() > 1) {
                for (int i = 1; i < trail.size(); i++) {
                    CelestialBody.Point3D p0 = trail.get(i-1);
                    CelestialBody.Point3D p1 = trail.get(i);
                    
                    double perspectiveScale0 = 1.0 / (1.0 + Math.abs(p0.z) * 0.08);
                    double perspectiveScale1 = 1.0 / (1.0 + Math.abs(p1.z) * 0.08);
                    
                    int sx0 = cx + (int)(p0.x * scale * zoom / 100 * perspectiveScale0);
                    int sy0 = cy + (int)(p0.y * scale * zoom / 100 * perspectiveScale0);
                    int sx1 = cx + (int)(p1.x * scale * zoom / 100 * perspectiveScale1);
                    int sy1 = cy + (int)(p1.y * scale * zoom / 100 * perspectiveScale1);
                    
                    g2d.drawLine(sx0, sy0, sx1, sy1);
                }
            }
        }
    }
    
    private void drawSun(Graphics2D g2d, int cx, int cy) {
        int baseSunSize = Math.max(15, (int)(zoom / 4));
        int sunSize = (int)(baseSunSize * getSizeRatio(sun));
        
        if (selectedPlanet == sun && isPaused) {
            g2d.setColor(new Color(255, 255, 255, 100));
            int highlightSize = sunSize + 8;
            g2d.fillOval(cx - highlightSize/2, cy - highlightSize/2, highlightSize, highlightSize);
        }
        
        RadialGradientPaint gradient = new RadialGradientPaint(
            cx, cy, sunSize/2,
            new float[]{0.0f, 0.7f, 1.0f},
            new Color[]{sun.getColor(), sun.getColor().darker(), sun.getColor().darker().darker()}
        );
        g2d.setPaint(gradient);
        g2d.fillOval(cx - sunSize/2, cy - sunSize/2, sunSize, sunSize);
        
        if (showLabels) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("Sun", cx + sunSize/2 + 5, cy + 4);
        }
    }
    
    private void drawPlanets(Graphics2D g2d, int cx, int cy) {
        List<CelestialBody> sortedPlanets = new ArrayList<>(planets);
        sortedPlanets.sort((p1, p2) -> {
            CelestialBody.Point3D pos1 = p1.getRotatedPosition(rotationX, rotationY, rotationZ);
            CelestialBody.Point3D pos2 = p2.getRotatedPosition(rotationX, rotationY, rotationZ);
            return Double.compare(pos2.z, pos1.z);
        });
        
        for (CelestialBody planet : sortedPlanets) {
            drawPlanet(g2d, planet, cx, cy);
        }
    }
    
    private void drawPlanet(Graphics2D g2d, CelestialBody planet, int cx, int cy) {
        CelestialBody.Point3D pos = planet.getRotatedPosition(rotationX, rotationY, rotationZ);
        double perspectiveScale = 1.0 / (1.0 + Math.abs(pos.z) * 0.08);
        int px = cx + (int)(pos.x * scale * zoom / 100 * perspectiveScale);
        int py = cy + (int)(pos.y * scale * zoom / 100 * perspectiveScale);
        
        int baseSunSize = Math.max(15, (int)(zoom / 4));
        double sizeRatio = getSizeRatio(planet);
        int planetSize = (int)(baseSunSize * sizeRatio * perspectiveScale);
        planetSize = Math.max(planetSize, getMinimumSize(planet));
        
        if (selectedPlanet == planet && isPaused) {
            g2d.setColor(new Color(255, 255, 255, 150));
            int highlightSize = planetSize + 6;
            g2d.fillOval(px - highlightSize/2, py - highlightSize/2, highlightSize, highlightSize);
        }
        
        Color baseColor = planet.getColor();
        float brightness = (float)Math.max(0.4, Math.min(1.0, perspectiveScale));
        Color shaded = new Color(
            (int)(baseColor.getRed() * brightness),
            (int)(baseColor.getGreen() * brightness),
            (int)(baseColor.getBlue() * brightness)
        );
        
        g2d.setColor(shaded);
        g2d.fillOval(px - planetSize/2, py - planetSize/2, planetSize, planetSize);
        
        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.fillOval(px - planetSize/2, py - planetSize/2, planetSize/3, planetSize/3);
        
        if (showLabels) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(planet.getName(), px + planetSize/2 + 3, py + 3);
        }
    }
    
    private void drawPlanetAttributes(Graphics2D g2d) {
        int panelWidth = 250;
        int panelHeight = 230;
        int panelX = getWidth() - panelWidth - 15;
        int panelY = 15;
        
        g2d.setColor(new Color(20, 20, 20, 230));
        g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);
        
        g2d.setColor(new Color(100, 150, 255, 180));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String title = selectedPlanet.getName() + " Properties";
        g2d.drawString(title, panelX + 10, panelY + 25);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        int yOffset = 55;
        int lineHeight = 16;
        
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawString("Mass:", panelX + 15, panelY + yOffset);
        g2d.setColor(Color.WHITE);
        g2d.drawString(formatMass(selectedPlanet.getMass()), panelX + 80, panelY + yOffset);
        yOffset += lineHeight;
        
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawString("Radius:", panelX + 15, panelY + yOffset);
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.format("%.1f km", selectedPlanet.getRadius()), panelX + 80, panelY + yOffset);
        yOffset += lineHeight;
        
        if (!selectedPlanet.getName().equals("Sun")) {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawString("Distance:", panelX + 15, panelY + yOffset);
            g2d.setColor(Color.WHITE);
            g2d.drawString(String.format("%.2f AU", selectedPlanet.getSemiMajorAxis()), panelX + 80, panelY + yOffset);
            yOffset += lineHeight;
            
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawString("Eccentricity:", panelX + 15, panelY + yOffset);
            g2d.setColor(Color.WHITE);
            g2d.drawString(String.format("%.3f", selectedPlanet.getEccentricity()), panelX + 80, panelY + yOffset);
            yOffset += lineHeight;
            
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawString("Orbital Period:", panelX + 15, panelY + yOffset);
            g2d.setColor(Color.WHITE);
            g2d.drawString(String.format("%.2f years", selectedPlanet.getOrbitalPeriod()), panelX + 80, panelY + yOffset);
            yOffset += lineHeight;
            
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawString("Inclination:", panelX + 15, panelY + yOffset);
            g2d.setColor(Color.WHITE);
            g2d.drawString(String.format("%.1f°", Math.toDegrees(selectedPlanet.getInclination())), panelX + 80, panelY + yOffset);
            yOffset += lineHeight;
            
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawString("Position:", panelX + 15, panelY + yOffset);
            g2d.setColor(Color.WHITE);
            g2d.drawString(String.format("(%.2f, %.2f) AU", selectedPlanet.getX(), selectedPlanet.getY()), 
                          panelX + 80, panelY + yOffset);
        }
        
        int buttonWidth = 100;
        int buttonHeight = 30;
        int buttonX = panelX + (panelWidth - buttonWidth) / 2;
        int buttonY = panelY + panelHeight - buttonHeight - 10;
        
        g2d.setColor(new Color(70, 130, 180));
        g2d.fillRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        String buttonText = "Edit Properties";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = buttonX + (buttonWidth - fm.stringWidth(buttonText)) / 2;
        int textY = buttonY + ((buttonHeight - fm.getHeight()) / 2) + fm.getAscent();
        g2d.drawString(buttonText, textX, textY);
    }
    
    public void openEditDialog() {
        if (selectedPlanet == null) return;
        
        JDialog dialog = new JDialog();
        dialog.setTitle("Edit " + selectedPlanet.getName());
        dialog.setModal(true);
        dialog.setSize(350, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField massField = new JTextField(String.valueOf(selectedPlanet.getMass()));
        JTextField radiusField = new JTextField(String.valueOf(selectedPlanet.getRadius()));
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Mass (kg):"), gbc);
        gbc.gridx = 1;
        panel.add(massField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Radius (km):"), gbc);
        gbc.gridx = 1;
        panel.add(radiusField, gbc);
        
        JTextField semiMajorField = null;
        JTextField eccentricityField = null;
        JTextField orbitalPeriodField = null;
        JTextField inclinationField = null;
        
        if (!selectedPlanet.getName().equals("Sun")) {
            semiMajorField = new JTextField(String.valueOf(selectedPlanet.getSemiMajorAxis()));
            eccentricityField = new JTextField(String.valueOf(selectedPlanet.getEccentricity()));
            orbitalPeriodField = new JTextField(String.valueOf(selectedPlanet.getOrbitalPeriod()));
            inclinationField = new JTextField(String.valueOf(Math.toDegrees(selectedPlanet.getInclination())));
            
            gbc.gridx = 0; gbc.gridy = 2;
            panel.add(new JLabel("Semi-Major Axis (AU):"), gbc);
            gbc.gridx = 1;
            panel.add(semiMajorField, gbc);
            
            gbc.gridx = 0; gbc.gridy = 3;
            panel.add(new JLabel("Eccentricity:"), gbc);
            gbc.gridx = 1;
            panel.add(eccentricityField, gbc);
            
            gbc.gridx = 0; gbc.gridy = 4;
            panel.add(new JLabel("Orbital Period (years):"), gbc);
            gbc.gridx = 1;
            panel.add(orbitalPeriodField, gbc);
            
            gbc.gridx = 0; gbc.gridy = 5;
            panel.add(new JLabel("Inclination (degrees):"), gbc);
            gbc.gridx = 1;
            panel.add(inclinationField, gbc);
        }
        
        JButton colorButton = new JButton("Change Color");
        colorButton.setBackground(selectedPlanet.getColor());
        
        final JTextField finalSemiMajor = semiMajorField;
        final JTextField finalEccentricity = eccentricityField;
        final JTextField finalOrbitalPeriod = orbitalPeriodField;
        final JTextField finalInclination = inclinationField;
        
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(dialog, "Choose Planet Color", selectedPlanet.getColor());
            if (newColor != null) {
                colorButton.setBackground(newColor);
            }
        });
        
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        panel.add(colorButton, gbc);
        
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            try {
                selectedPlanet.setMass(Double.parseDouble(massField.getText()));
                selectedPlanet.setRadius(Double.parseDouble(radiusField.getText()));
                selectedPlanet.setColor(colorButton.getBackground());
                
                if (!selectedPlanet.getName().equals("Sun")) {
                    selectedPlanet.setSemiMajorAxis(Double.parseDouble(finalSemiMajor.getText()));
                    selectedPlanet.setEccentricity(Double.parseDouble(finalEccentricity.getText()));
                    selectedPlanet.setOrbitalPeriod(Double.parseDouble(finalOrbitalPeriod.getText()));
                    selectedPlanet.setInclination(Double.parseDouble(finalInclination.getText()));
                }
                
                repaint();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid numbers!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private String formatMass(double mass) {
        if (mass >= 1e24) {
            return String.format("%.2e kg", mass);
        } else {
            return String.format("%.1e kg", mass);
        }
    }
    
    private void drawInfoPanel(Graphics2D g2d) {
        g2d.setColor(new Color(30, 30, 30, 210));
        g2d.fillRoundRect(15, 18, 320, 100, 12, 12);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        String timeString = String.format("Simulation Time: %.1f Earth years", simulationTime);
        g2d.drawString(timeString, 30, 40);
        
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.drawString("Spacebar=Play/Pause, R=Reset, O=Orbits, L=Labels", 30, 60);
        g2d.drawString("Mouse+Wheel=View, Arrow Keys=Speed", 30, 75);
        
        if (isPaused) {
            g2d.setColor(Color.YELLOW);
            g2d.drawString("PAUSED - Click on planets/sun to view properties", 30, 95);
        }
    }
}