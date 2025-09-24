import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.awt.event.MouseEvent;

public class SolarSystemPanel extends JPanel {
    private List<CelestialBody> planets;
    private CelestialBody sun;
    private double rotationX = 0;
    private double rotationY = 0;
    private double rotationZ = 0;
    private double zoom = 80;
    private double scale = 60;
    private double simulationTime = 0;
    private static final double TIME_STEP = 0.006;
    private boolean showOrbits = true;
    private boolean showLabels = true;
    private Color orbitColor = new Color(0, 255, 0, 60);

    // Planet selection functionality
    private CelestialBody selectedPlanet = null;
    private boolean isPaused = false;

    // Size ratios relative to Sun
    private static final double SUN_RATIO = 1.0;
    private static final double MERCURY_RATIO = 0.0035;
    private static final double VENUS_RATIO = 0.0087;
    private static final double EARTH_RATIO = 0.0091;
    private static final double MARS_RATIO = 0.0049;
    private static final double JUPITER_RATIO = 0.1004;
    private static final double SATURN_RATIO = 0.0836;
    private static final double URANUS_RATIO = 0.0364;
    private static final double NEPTUNE_RATIO = 0.0354;

    public SolarSystemPanel() {
        setBackground(Color.BLACK);
        setFocusable(true);
        setPreferredSize(new Dimension(1200, 900));
        initializeSolarSystem();
    }

    private void initializeSolarSystem() {
        planets = new ArrayList<>();
        sun = new CelestialBody("Sun", CelestialBody.SOLAR_MASS, 696340, Color.YELLOW, 0, 0, 0, 0);

        planets.add(new CelestialBody("Mercury", 3.301e23, 2439.7, Color.GRAY,
                              0.39, 0.206, 0.24, Math.toRadians(7.0)));
        planets.add(new CelestialBody("Venus", 4.867e24, 6051.8, new Color(255,180,10),
                              0.72, 0.007, 0.62, Math.toRadians(3.4)));
        planets.add(new CelestialBody("Earth", 5.972e24, 6371, new Color(70,130,250),
                              1.0, 0.017, 1.0, Math.toRadians(0.0)));
        planets.add(new CelestialBody("Mars", 6.39e23, 3389.5, new Color(255,60,30),
                              1.52, 0.094, 1.88, Math.toRadians(1.9)));
        planets.add(new CelestialBody("Jupiter", 1.898e27, 69911, new Color(200,170,60),
                              5.20, 0.049, 11.86, Math.toRadians(1.3)));
        planets.add(new CelestialBody("Saturn", 5.683e26, 58232, new Color(210,180,140),
                              9.58, 0.057, 29.46, Math.toRadians(2.5)));
        planets.add(new CelestialBody("Uranus", 8.681e25, 25362, new Color(85,255,255),
                              19.22, 0.046, 84.01, Math.toRadians(0.8)));
        planets.add(new CelestialBody("Neptune", 1.024e26, 24622, new Color(60,130,255),
                              30.05, 0.010, 164.8, Math.toRadians(1.8)));
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

        selectedPlanet = null;

        // Check if sun was clicked
        int baseSunSize = Math.max(15, (int)(zoom / 4));
        int sunSize = (int)(baseSunSize * SUN_RATIO);
        if (isPointInCircle(clickX, clickY, centerX, centerY, sunSize/2)) {
            selectedPlanet = sun;
            repaint();
            return;
        }

        // Check planets
        for (CelestialBody planet : planets) {
            CelestialBody.Point3D pos = planet.getRotatedPosition(rotationX, rotationY, rotationZ);
            double perspectiveScale = 1.0 / (1.0 + Math.abs(pos.z) * 0.08);
            int px = centerX + (int)(pos.x * scale * zoom / 100 * perspectiveScale);
            int py = centerY + (int)(pos.y * scale * zoom / 100 * perspectiveScale);

            String planetName = planet.getName();
            double sizeRatio = getSizeRatio(planetName);
            int planetSize = (int)(baseSunSize * sizeRatio * perspectiveScale);
            planetSize = Math.max(planetSize, getMinimumSize(planetName));

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

    private double getSizeRatio(String planetName) {
        switch (planetName) {
            case "Mercury": return MERCURY_RATIO;
            case "Venus": return VENUS_RATIO;
            case "Earth": return EARTH_RATIO;
            case "Mars": return MARS_RATIO;
            case "Jupiter": return JUPITER_RATIO;
            case "Saturn": return SATURN_RATIO;
            case "Uranus": return URANUS_RATIO;
            case "Neptune": return NEPTUNE_RATIO;
            default: return 0.01;
        }
    }

    private int getMinimumSize(String planetName) {
        if (planetName.equals("Jupiter")) return 6;
        if (planetName.equals("Saturn")) return 5;
        if (planetName.equals("Uranus") || planetName.equals("Neptune")) return 4;
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
            java.util.List<CelestialBody.Point3D> trail = planet.getRotatedTrail(rotationX, rotationY, rotationZ);

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

                CelestialBody.Point3D lastTrailPoint = trail.get(trail.size() - 1);
                CelestialBody.Point3D currentPos = planet.getRotatedPosition(rotationX, rotationY, rotationZ);

                double perspectiveScaleTrail = 1.0 / (1.0 + Math.abs(lastTrailPoint.z) * 0.08);
                double perspectiveScaleCurrent = 1.0 / (1.0 + Math.abs(currentPos.z) * 0.08);

                int trailX = cx + (int)(lastTrailPoint.x * scale * zoom / 100 * perspectiveScaleTrail);
                int trailY = cy + (int)(lastTrailPoint.y * scale * zoom / 100 * perspectiveScaleTrail);
                int planetX = cx + (int)(currentPos.x * scale * zoom / 100 * perspectiveScaleCurrent);
                int planetY = cy + (int)(currentPos.y * scale * zoom / 100 * perspectiveScaleCurrent);

                g2d.drawLine(trailX, trailY, planetX, planetY);
            }
        }
    }

    private void drawSun(Graphics2D g2d, int cx, int cy) {
        int baseSunSize = Math.max(15, (int)(zoom / 4));
        int sunSize = (int)(baseSunSize * SUN_RATIO);

        if (selectedPlanet == sun && isPaused) {
            g2d.setColor(new Color(255, 255, 255, 100));
            int highlightSize = sunSize + 8;
            g2d.fillOval(cx - highlightSize/2, cy - highlightSize/2, highlightSize, highlightSize);
        }

        RadialGradientPaint gradient = new RadialGradientPaint(
            cx, cy, sunSize/2,
            new float[]{0.0f, 0.7f, 1.0f},
            new Color[]{Color.YELLOW, Color.ORANGE, new Color(255, 140, 0)}
        );
        g2d.setPaint(gradient);
        g2d.fillOval(cx - sunSize/2, cy - sunSize/2, sunSize, sunSize);

        g2d.setColor(new Color(255, 255, 0, 30));
        int glowSize = sunSize + 4;
        g2d.fillOval(cx - glowSize/2, cy - glowSize/2, glowSize, glowSize);

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
        String planetName = planet.getName();
        double sizeRatio = getSizeRatio(planetName);
        int planetSize = (int)(baseSunSize * sizeRatio * perspectiveScale);
        planetSize = Math.max(planetSize, getMinimumSize(planetName));

        if (selectedPlanet == planet && isPaused) {
            g2d.setColor(new Color(255, 255, 255, 150));
            int highlightSize = planetSize + 6;
            g2d.fillOval(px - highlightSize/2, py - highlightSize/2, highlightSize, highlightSize);
        }

        Color baseColor = planet.getColor();
        float brightness = (float)Math.max(0.4, Math.min(1.0, perspectiveScale));
        Color shaded = new Color(
            (int)(baseColor.getRed()*brightness),
            (int)(baseColor.getGreen()*brightness),
            (int)(baseColor.getBlue()*brightness)
        );

        g2d.setColor(shaded);
        g2d.fillOval(px-planetSize/2, py-planetSize/2, planetSize, planetSize);

        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.fillOval(px-planetSize/2, py-planetSize/2, planetSize/3, planetSize/3);

        if (showLabels) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(planet.getName(), px+planetSize/2+3, py+3);
        }
    }

    private void drawPlanetAttributes(Graphics2D g2d) {
        int panelWidth = 250;
        int panelHeight = 200;
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

        g2d.setColor(new Color(100, 150, 255, 100));
        g2d.drawLine(panelX + 10, panelY + 35, panelX + panelWidth - 10, panelY + 35);

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

        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Arial", Font.ITALIC, 10));
        g2d.drawString("Click elsewhere to close", panelX + 15, panelY + panelHeight - 15);
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

        g2d.setColor(Color.CYAN);
        // g2d.drawString("Sizes use actual solar system ratios", 30, 95);

        if (isPaused) {
            g2d.setColor(Color.YELLOW);
            g2d.drawString("PAUSED - Click on planets/sun to view properties", 30, 110);
        }
    }
}