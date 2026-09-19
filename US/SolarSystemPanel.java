import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
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

    // Gesture control state
    private int cursorX = -1, cursorY = -1;
    private boolean showCursor = false;
    private boolean gestureSelected = false;
    private String gestureStatus = "";
    private double displaySpeed = 1.0;

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
        setPreferredSize(new Dimension(1200, 900));
        initializeSolarSystem();
    }

    private void initializeSolarSystem() {
        sun = new CelestialBody("Sun", CelestialBody.SOLAR_MASS, 696340, Color.YELLOW, 0, 0, 0, 0);

        planets.add(new CelestialBody("Mercury", 3.301e23, 2439.7, Color.GRAY,
                              0.39, 0.206, 0.24, 7.0));
        planets.add(new CelestialBody("Venus", 4.867e24, 6051.8, new Color(255,180,10),
                              0.72, 0.007, 0.62, 3.4));
        planets.add(new CelestialBody("Earth", 5.972e24, 6371, new Color(70,130,250),
                              1.0, 0.017, 1.0, 0.0));
        planets.add(new CelestialBody("Mars", 6.39e23, 3389.5, new Color(255,60,30),
                              1.52, 0.094, 1.88, 1.9));
        planets.add(new CelestialBody("Jupiter", 1.898e27, 69911, new Color(200,170,60),
                              5.20, 0.049, 11.86, 1.3));
        planets.add(new CelestialBody("Saturn", 5.683e26, 58232, new Color(210,180,140),
                              9.58, 0.057, 29.46, 2.5));
        planets.add(new CelestialBody("Uranus", 8.681e25, 25362, new Color(85,255,255),
                              19.22, 0.046, 84.01, 0.8));
        planets.add(new CelestialBody("Neptune", 1.024e26, 24622, new Color(60,130,255),
                              30.05, 0.010, 164.8, 1.8));
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

    // ── Gesture control methods ──────────────────────────────
    public void setVirtualCursor(int x, int y) {
        this.cursorX = x;
        this.cursorY = y;
        this.showCursor = true;
    }

    public void hideVirtualCursor() {
        this.showCursor = false;
        this.cursorX = -1;
        this.cursorY = -1;
    }

    public void selectPlanetAt(int x, int y) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        selectedPlanet = null;
        gestureSelected = false;
        int baseSunSize = Math.max(15, (int)(zoom / 4));
        int sunSize = (int)(baseSunSize * SUN_RATIO);
        if (isPointInCircle(x, y, centerX, centerY, sunSize / 2 + 5)) {
            selectedPlanet = sun;
            gestureSelected = true;
            repaint();
            return;
        }
        for (CelestialBody planet : planets) {
            CelestialBody.Point3D pos = planet.getRotatedPosition(rotationX, rotationY, rotationZ);
            double perspectiveScale = 1.0 / (1.0 + Math.abs(pos.z) * 0.08);
            int px = centerX + (int)(pos.x * scale * zoom / 100 * perspectiveScale);
            int py = centerY + (int)(pos.y * scale * zoom / 100 * perspectiveScale);
            String planetName = planet.getName();
            double sizeRatio = getSizeRatio(planetName);
            int planetSize = (int)(baseSunSize * sizeRatio * perspectiveScale);
            planetSize = Math.max(planetSize, getMinimumSize(planetName));
            if (isPointInCircle(x, y, px, py, planetSize / 2 + 12)) {
                selectedPlanet = planet;
                gestureSelected = true;
                repaint();
                return;
            }
        }
        repaint();
    }

    public void clearGestureSelection() {
        gestureSelected = false;
        selectedPlanet = null;
    }

    public void setGestureStatus(String status) {
        this.gestureStatus = status;
    }

    public void setDisplaySpeed(double speed) {
        this.displaySpeed = speed;
    }

    public void handleMouseClick(MouseEvent e) {
        if (!isPaused) return;
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int clickX = e.getX();
        int clickY = e.getY();
        selectedPlanet = null;
        int baseSunSize = Math.max(15, (int)(zoom / 4));
        int sunSize = (int)(baseSunSize * SUN_RATIO);
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
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        drawOrbits(g2d, centerX, centerY);
        drawSun(g2d, centerX, centerY);
        drawPlanets(g2d, centerX, centerY);
        drawInfoPanel(g2d);
        if (selectedPlanet != null && (isPaused || gestureSelected)) {
            drawPlanetAttributes(g2d);
        }
        if (showCursor) {
            drawVirtualCursor(g2d);
        }
    }

    private void drawOrbits(Graphics2D g2d, int cx, int cy) {
        if (!showOrbits) return;
        g2d.setColor(orbitColor);
        g2d.setStroke(new BasicStroke(1.5f));
        for (CelestialBody planet : planets) {
            List<CelestialBody.Point3D> trail = planet.getRotatedTrail(rotationX, rotationY, rotationZ);
            if (trail.size() > 1) {
                // One Path2D per planet instead of one drawLine() call per
                // trail segment (up to 200 draw calls per planet per frame).
                Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO, trail.size());
                boolean started = false;
                for (CelestialBody.Point3D p : trail) {
                    double perspectiveScale = 1.0 / (1.0 + Math.abs(p.z) * 0.08);
                    double sx = cx + p.x * scale * zoom / 100 * perspectiveScale;
                    double sy = cy + p.y * scale * zoom / 100 * perspectiveScale;
                    if (!started) {
                        path.moveTo(sx, sy);
                        started = true;
                    } else {
                        path.lineTo(sx, sy);
                    }
                }
                g2d.draw(path);
            }
        }
    }

    private void drawSun(Graphics2D g2d, int cx, int cy) {
        int baseSunSize = Math.max(15, (int)(zoom / 4));
        double sunSize = baseSunSize * SUN_RATIO;
        if (selectedPlanet == sun && (isPaused || gestureSelected)) {
            g2d.setColor(new Color(255, 255, 255, 100));
            double highlightSize = sunSize + 8;
            g2d.fill(new Ellipse2D.Double(cx - highlightSize/2, cy - highlightSize/2, highlightSize, highlightSize));
        }
        RadialGradientPaint gradient = new RadialGradientPaint(
            cx, cy, (float)(sunSize/2),
            new float[]{0.0f, 0.7f, 1.0f},
            new Color[]{Color.YELLOW, Color.ORANGE, new Color(255, 140, 0)}
        );
        g2d.setPaint(gradient);
        g2d.fill(new Ellipse2D.Double(cx - sunSize/2, cy - sunSize/2, sunSize, sunSize));
        if (showLabels) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(UiFonts.bold(12));
            g2d.drawString("Sun", (float)(cx + sunSize/2 + 5), (float)(cy + 4));
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
        double px = cx + pos.x * scale * zoom / 100 * perspectiveScale;
        double py = cy + pos.y * scale * zoom / 100 * perspectiveScale;
        int baseSunSize = Math.max(15, (int)(zoom / 4));
        String planetName = planet.getName();
        double sizeRatio = getSizeRatio(planetName);
        double planetSize = baseSunSize * sizeRatio * perspectiveScale;
        planetSize = Math.max(planetSize, getMinimumSize(planetName));
        if (selectedPlanet == planet && (isPaused || gestureSelected)) {
            g2d.setColor(new Color(255, 255, 255, 150));
            double highlightSize = planetSize + 6;
            g2d.fill(new Ellipse2D.Double(px - highlightSize/2, py - highlightSize/2, highlightSize, highlightSize));
        }
        Color baseColor = planet.getColor();
        float brightness = (float)Math.max(0.4, Math.min(1.0, perspectiveScale));
        Color shaded = new Color(
            (int)(baseColor.getRed() * brightness),
            (int)(baseColor.getGreen() * brightness),
            (int)(baseColor.getBlue() * brightness)
        );
        g2d.setColor(shaded);
        g2d.fill(new Ellipse2D.Double(px - planetSize/2, py - planetSize/2, planetSize, planetSize));
        g2d.setColor(new Color(255, 255, 255, 80));
        double highlightSize = planetSize / 3;
        g2d.fill(new Ellipse2D.Double(px - planetSize/2, py - planetSize/2, highlightSize, highlightSize));
        if (showLabels) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(UiFonts.bold(10));
            g2d.drawString(planet.getName(), (float)(px + planetSize/2 + 3), (float)(py + 3));
        }
    }

    private void drawPlanetAttributes(Graphics2D g2d) {
        boolean isSun = selectedPlanet.getName().equals("Sun");

        java.util.List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Mass:", formatMass(selectedPlanet.getMass())});
        rows.add(new String[]{"Radius:", String.format("%.1f km", selectedPlanet.getRadius())});
        if (!isSun) {
            rows.add(new String[]{"Distance:", String.format("%.2f AU", selectedPlanet.getSemiMajorAxis())});
            rows.add(new String[]{"Eccentricity:", String.format("%.3f", selectedPlanet.getEccentricity())});
            rows.add(new String[]{"Orbital Period:", String.format("%.2f years", selectedPlanet.getOrbitalPeriod())});
            rows.add(new String[]{"Inclination:", String.format("%.1f°", Math.toDegrees(selectedPlanet.getInclination()))});
            rows.add(new String[]{"Position:", String.format("(%.2f, %.2f) AU", selectedPlanet.getX(), selectedPlanet.getY())});
        }

        Font titleFont = UiFonts.bold(15);
        Font labelFont = UiFonts.plain(12);
        FontMetrics labelFm = g2d.getFontMetrics(labelFont);

        // Size the label column to the widest label instead of a fixed
        // offset, so long labels like "Orbital Period:" stop overlapping
        // their values.
        int labelColWidth = 0;
        for (String[] row : rows) {
            labelColWidth = Math.max(labelColWidth, labelFm.stringWidth(row[0]));
        }
        int padding = 16;
        int valueColX = padding + labelColWidth + 14;
        int lineHeight = 20;

        int panelWidth = 250;
        int panelHeight = 44 + rows.size() * lineHeight + 14;
        int panelX = getWidth() - panelWidth - 15;
        int panelY = 15;

        g2d.setColor(new Color(20, 20, 20, 230));
        g2d.fill(new RoundRectangle2D.Double(panelX, panelY, panelWidth, panelHeight, 12, 12));
        g2d.setColor(new Color(100, 150, 255, 180));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(new RoundRectangle2D.Double(panelX, panelY, panelWidth, panelHeight, 12, 12));

        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        String title = selectedPlanet.getName() + " Properties";
        g2d.drawString(title, panelX + padding, panelY + 28);

        g2d.setFont(labelFont);
        int y = panelY + 52;
        for (String[] row : rows) {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawString(row[0], panelX + padding, y);
            g2d.setColor(Color.WHITE);
            g2d.drawString(row[1], panelX + valueColX, y);
            y += lineHeight;
        }
    }

    private String formatMass(double mass) {
        if (mass >= 1e24) {
            return String.format("%.2e kg", mass);
        } else {
            return String.format("%.1e kg", mass);
        }
    }

    private void drawVirtualCursor(Graphics2D g2d) {
        int size = 22;
        int half = size / 2;
        g2d.setColor(new Color(0, 255, 200, 40));
        g2d.fill(new Ellipse2D.Double(cursorX - size, cursorY - size, size * 2, size * 2));
        g2d.setColor(new Color(0, 255, 200, 200));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.draw(new Ellipse2D.Double(cursorX - half, cursorY - half, size, size));
        int ext = size + 6;
        g2d.drawLine(cursorX - ext, cursorY, cursorX - half - 3, cursorY);
        g2d.drawLine(cursorX + half + 3, cursorY, cursorX + ext, cursorY);
        g2d.drawLine(cursorX, cursorY - ext, cursorX, cursorY - half - 3);
        g2d.drawLine(cursorX, cursorY + half + 3, cursorX, cursorY + ext);
        g2d.setColor(new Color(0, 255, 200, 255));
        g2d.fill(new Ellipse2D.Double(cursorX - 2, cursorY - 2, 4, 4));
        g2d.setFont(UiFonts.plain(11));
        g2d.setColor(new Color(0, 255, 200, 180));
        g2d.drawString("BLINK to select", cursorX + ext + 4, cursorY + 4);
    }

    private void drawInfoPanel(Graphics2D g2d) {
        Font titleFont = UiFonts.bold(14);
        Font bodyFont = UiFonts.plain(12);
        Font gestureFont = UiFonts.bold(13);

        String timeString = String.format("Simulation Time: %.1f Earth years", simulationTime);
        String speedLine = String.format("Speed: %.1fx   |   Keyboard + Mouse + Gestures", displaySpeed);
        String keysLine = "Spacebar = Play/Pause   R = Reset   O = Orbits   L = Labels";
        String pausedLine = "PAUSED - Click or blink at planets to view properties";
        String gestureLine = "Gesture: " + gestureStatus;

        FontMetrics titleFm = g2d.getFontMetrics(titleFont);
        FontMetrics bodyFm = g2d.getFontMetrics(bodyFont);
        FontMetrics gestureFm = g2d.getFontMetrics(gestureFont);

        int widest = titleFm.stringWidth(timeString);
        widest = Math.max(widest, bodyFm.stringWidth(speedLine));
        widest = Math.max(widest, bodyFm.stringWidth(keysLine));
        if (isPaused) widest = Math.max(widest, bodyFm.stringWidth(pausedLine));
        if (!gestureStatus.isEmpty()) widest = Math.max(widest, gestureFm.stringWidth(gestureLine));

        int panelX = 15, panelY = 18;
        int padding = 15;
        int panelWidth = widest + padding * 2;
        int lineHeight = 20;
        int lineCount = 3 + (isPaused ? 1 : 0) + (gestureStatus.isEmpty() ? 0 : 1);
        int panelHeight = 20 + lineCount * lineHeight;

        g2d.setColor(new Color(30, 30, 30, 210));
        g2d.fill(new RoundRectangle2D.Double(panelX, panelY, panelWidth, panelHeight, 12, 12));
        g2d.setColor(new Color(90, 90, 110, 160));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(new RoundRectangle2D.Double(panelX, panelY, panelWidth, panelHeight, 12, 12));

        int textX = panelX + padding;
        int y = panelY + 26;
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        g2d.drawString(timeString, textX, y);
        y += lineHeight;

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(bodyFont);
        g2d.drawString(speedLine, textX, y);
        y += lineHeight;
        g2d.drawString(keysLine, textX, y);
        y += lineHeight;

        if (isPaused) {
            g2d.setColor(Color.YELLOW);
            g2d.drawString(pausedLine, textX, y);
            y += lineHeight;
        }
        if (!gestureStatus.isEmpty()) {
            g2d.setColor(new Color(0, 220, 180));
            g2d.setFont(gestureFont);
            g2d.drawString(gestureLine, textX, y);
        }
    }
}
