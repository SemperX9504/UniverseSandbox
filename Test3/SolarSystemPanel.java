package Test3;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;

public class SolarSystemPanel extends JPanel {
    private List<CelestialBody> planets = new ArrayList<>();
    private double rotationX = 0, rotationY = 0, zoom = 60, scale = 125, simulationTime = 0;
    private static final double TIME_STEP = 0.01;
    private boolean showOrbits = true, showLabels = true, paused = false;
    private CelestialBody selectedBody = null;
    private boolean sunSelected = false;

    // Communication
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final Gson gson = new Gson();

    public SolarSystemPanel() throws IOException {
        setBackground(Color.BLACK);
        initializePlanets();

        // Connect server
        socket = new Socket("localhost", 5050);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Connected to C++ physics engine!");
    }

    private void initializePlanets() {
        // Constructor: (name, color1, color2, displaySize, hasRings, mass, radius, semiMajor, eccentricity, period, inclination)
        
        // Mercury - small, gray/brown rocky planet
        planets.add(new CelestialBody("Mercury", 
            new Color(169, 169, 169), new Color(120, 120, 120), 4, false,
            3.3e23, 2439.7, 0.39, 0.206, 0.24, 7.0));
        
        // Venus - bright yellowish-white with thick atmosphere
        planets.add(new CelestialBody("Venus", 
            new Color(255, 198, 73), new Color(230, 180, 100), 8, false,
            4.87e24, 6051.8, 0.72, 0.007, 0.62, 3.4));
        
        // Earth - blue and green with white clouds
        planets.add(new CelestialBody("Earth", 
            new Color(65, 105, 225), new Color(34, 139, 34), 8, false,
            5.97e24, 6371, 1.0, 0.017, 1.0, 0.0));
        
        // Mars - reddish-orange rocky planet
        planets.add(new CelestialBody("Mars", 
            new Color(193, 68, 14), new Color(139, 69, 19), 6, false,
            6.42e23, 3389.5, 1.52, 0.093, 1.88, 1.9));
        
        // Jupiter - massive with bands, orange and brown
        planets.add(new CelestialBody("Jupiter", 
            new Color(216, 202, 157), new Color(186, 145, 79), 20, false,
            1.898e27, 69911, 5.20, 0.049, 11.86, 1.3));
        
        // Saturn - pale gold with iconic rings
        planets.add(new CelestialBody("Saturn", 
            new Color(238, 217, 130), new Color(205, 186, 99), 18, true,
            5.68e26, 58232, 9.58, 0.056, 29.46, 2.5));
        
        // Uranus - pale cyan/blue-green ice giant
        planets.add(new CelestialBody("Uranus", 
            new Color(79, 208, 231), new Color(130, 225, 240), 12, false,
            8.68e25, 25362, 19.22, 0.046, 84.01, 0.8));
        
        // Neptune - deep blue ice giant
        planets.add(new CelestialBody("Neptune", 
            new Color(72, 118, 255), new Color(31, 81, 255), 12, false,
            1.02e26, 24622, 30.05, 0.010, 164.8, 1.8));
    }

    public void updateSimulation(double speedMultiplier) {
        try {
            double dt = TIME_STEP * speedMultiplier;
            out.println(dt);
            out.flush();
            String json = in.readLine();
            if (json == null || json.isEmpty()) return;

            // Parse new JSON format with planet types (from polymorphism)
            com.google.gson.JsonObject jsonObj = gson.fromJson(json, com.google.gson.JsonObject.class);
            com.google.gson.JsonArray planetsArray = jsonObj.getAsJsonArray("planets");
            
            for (int i = 0; i < planets.size() && i < planetsArray.size(); i++) {
                com.google.gson.JsonObject planetData = planetsArray.get(i).getAsJsonObject();
                com.google.gson.JsonArray pos = planetData.getAsJsonArray("pos");
                double gravity = planetData.get("gravity").getAsDouble();
                String type = planetData.get("type").getAsString();
                
                planets.get(i).setPosition(pos.get(0).getAsDouble(), 
                                          pos.get(1).getAsDouble(), 
                                          pos.get(2).getAsDouble(),
                                          gravity, type);
            }
            simulationTime += dt;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetSimulation() {
        try {
            // Send RESET command to C++ server
            out.println("RESET");
            out.flush();
            
            // Read initial positions from server
            String json = in.readLine();
            if (json != null && !json.isEmpty()) {
            // Parse new JSON format with planet types (from polymorphism)
            com.google.gson.JsonObject jsonObj = gson.fromJson(json, com.google.gson.JsonObject.class);
            com.google.gson.JsonArray planetsArray = jsonObj.getAsJsonArray("planets");
            
            for (int i = 0; i < planets.size() && i < planetsArray.size(); i++) {
                com.google.gson.JsonObject planetData = planetsArray.get(i).getAsJsonObject();
                com.google.gson.JsonArray pos = planetData.getAsJsonArray("pos");
                double gravity = planetData.get("gravity").getAsDouble();
                String type = planetData.get("type").getAsString();
                
                planets.get(i).setPosition(pos.get(0).getAsDouble(), 
                                          pos.get(1).getAsDouble(), 
                                          pos.get(2).getAsDouble(),
                                          gravity, type);
            }
            }
            
            // Reset visualization
            simulationTime = 0;
            zoom = 60; // Reset to default zoom (60%)
            rotationX = 0;
            rotationY = 0;
            planets.forEach(CelestialBody::reset);
            repaint();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void rotateView(double dx, double dy) {
        rotationY += dx;
        rotationX += dy;
        repaint();
    }

    public void adjustZoom(double delta) {
        // Zoom in increments of exactly 10
        // delta is typically +1 (zoom out) or -1 (zoom in) from mouse wheel
        
        if (delta > 0) {
            // Zoom out - decrease by 10
            zoom -= 10;
        } else if (delta < 0) {
            // Zoom in - increase by 10
            zoom += 10;
        }
        
        // Clamp to range [10, 500]
        zoom = Math.max(10, Math.min(500, zoom));
        
        // Ensure it's a multiple of 10
        zoom = Math.round(zoom / 10.0) * 10;
        
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

    public void setPaused(boolean p) {
        paused = p;
        if (!paused) {
            // Clear selection when resuming
            selectedBody = null;
            sunSelected = false;
        }
    }
    
    public void handleClick(int mouseX, int mouseY) {
        if (!paused) return; // Only allow clicking when paused
        
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        
        // Check if Sun was clicked
        int sunSize = (int)(25 * zoom / 100);
        sunSize = Math.max(5, Math.min(sunSize, 200));
        double distToSun = Math.sqrt(Math.pow(mouseX - cx, 2) + Math.pow(mouseY - cy, 2));
        if (distToSun <= sunSize) {
            sunSelected = true;
            selectedBody = null;
            repaint();
            return;
        }
        
        // Check which planet was clicked (check in reverse order to match rendering)
        selectedBody = null;
        sunSelected = false;
        
        for (CelestialBody planet : planets) {
            CelestialBody.Point3D pos = planet.getRotatedPosition(rotationX, rotationY);
            int x = cx + (int) (pos.x * scale * zoom / 100);
            int y = cy + (int) (pos.y * scale * zoom / 100);
            int scaledSize = (int)(planet.getDisplaySize() * zoom / 100);
            scaledSize = Math.max(2, Math.min(scaledSize, 150));
            
            double dist = Math.sqrt(Math.pow(mouseX - x, 2) + Math.pow(mouseY - y, 2));
            if (dist <= scaledSize) {
                selectedBody = planet;
                sunSelected = false;
                break;
            }
        }
        
        repaint();
    }

    private void drawSun(Graphics2D g2d, int cx, int cy, int size) {
        // Outer glow
        for (int i = 5; i > 0; i--) {
            int glowSize = size + i * 8;
            int alpha = 15 * i;
            g2d.setColor(new Color(255, 200, 0, alpha));
            g2d.fillOval(cx - glowSize/2, cy - glowSize/2, glowSize, glowSize);
        }
        
        // Main sun body with radial gradient
        RadialGradientPaint gradient = new RadialGradientPaint(
            cx, cy, size,
            new float[]{0.0f, 0.7f, 1.0f},
            new Color[]{
                new Color(255, 255, 200),  // Bright center
                new Color(255, 220, 0),     // Yellow middle
                new Color(255, 150, 0)      // Orange edge
            }
        );
        g2d.setPaint(gradient);
        g2d.fillOval(cx - size, cy - size, size * 2, size * 2);
        
        // Bright core
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillOval(cx - size/3, cy - size/3, size*2/3, size*2/3);
    }
    
    private void drawPlanet(Graphics2D g2d, CelestialBody planet, int x, int y, double z, int size) {
        // Size is now passed as parameter (already scaled by zoom)
        
        // Draw rings for Saturn (behind the planet)
        if (planet.hasRings() && z < 0) {
            drawSaturnRings(g2d, x, y, size);
        }
        
        // Atmospheric glow for gas giants
        if (size >= 12) {
            for (int i = 3; i > 0; i--) {
                int glowSize = size + i * 3;
                int alpha = 20;
                Color glowColor = new Color(
                    planet.getColor().getRed(), 
                    planet.getColor().getGreen(), 
                    planet.getColor().getBlue(), 
                    alpha
                );
                g2d.setColor(glowColor);
                g2d.fillOval(x - glowSize/2, y - glowSize/2, glowSize, glowSize);
            }
        }
        
        // Planet body with gradient for 3D effect
        RadialGradientPaint gradient = new RadialGradientPaint(
            x - size/4, y - size/4, size * 1.2f,
            new float[]{0.0f, 0.6f, 1.0f},
            new Color[]{
                planet.getSecondaryColor().brighter(),  // Bright side (sunlit)
                planet.getColor(),                       // Main color
                planet.getColor().darker()               // Dark side (shadow)
            }
        );
        g2d.setPaint(gradient);
        g2d.fillOval(x - size, y - size, size * 2, size * 2);
        
        // Specular highlight for rocky planets (smaller ones)
        if (size < 12) {
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fillOval(x - size/2, y - size/2, size/2, size/2);
        }
        
        // Draw rings for Saturn (in front of the planet)
        if (planet.hasRings() && z >= 0) {
            drawSaturnRings(g2d, x, y, size);
        }
    }
    
    private void drawSaturnRings(Graphics2D g2d, int x, int y, int size) {
        g2d.setStroke(new BasicStroke(1.5f));
        
        // Draw multiple ring layers
        int[] ringRadii = {size + 6, size + 10, size + 14};
        int[] alphas = {120, 80, 50};
        Color ringColor = new Color(210, 180, 140);
        
        for (int i = 0; i < ringRadii.length; i++) {
            int radius = ringRadii[i];
            int alpha = alphas[i];
            
            // Draw ellipse for 3D perspective
            g2d.setColor(new Color(ringColor.getRed(), ringColor.getGreen(), 
                                   ringColor.getBlue(), alpha));
            g2d.drawOval(x - radius, y - radius/4, radius * 2, radius/2);
            
            // Fill the ring with transparency
            g2d.setColor(new Color(ringColor.getRed(), ringColor.getGreen(), 
                                   ringColor.getBlue(), alpha/3));
            g2d.fillOval(x - radius, y - radius/4, radius * 2, radius/2);
        }
        
        g2d.setStroke(new BasicStroke(1.0f));
    }
    
    // Helper class for z-depth sorting
    private static class PlanetRenderInfo {
        CelestialBody planet;
        CelestialBody.Point3D pos;
        int x, y;
        int scaledSize;
        
        PlanetRenderInfo(CelestialBody planet, CelestialBody.Point3D pos, int x, int y, int scaledSize) {
            this.planet = planet;
            this.pos = pos;
            this.x = x;
            this.y = y;
            this.scaledSize = scaledSize;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int cx = getWidth() / 2, cy = getHeight() / 2;

        // Draw Trails first (always in background)
        if (showOrbits) {
            g2d.setStroke(new BasicStroke(1.2f));
            for (CelestialBody planet : planets) {
                var rotatedTrail = CelestialBody.getRotatedTrail(planet.getTrail(), rotationX, rotationY);
                CelestialBody.Point3D prev = null;
                int alpha = 40; // base transparency
                for (CelestialBody.Point3D p : rotatedTrail) {
                    if (prev != null) {
                        int x1 = cx + (int) (prev.x * scale * zoom / 100);
                        int y1 = cy + (int) (prev.y * scale * zoom / 100);
                        int x2 = cx + (int) (p.x * scale * zoom / 100);
                        int y2 = cy + (int) (p.y * scale * zoom / 100);
                        g2d.setColor(new Color(0, 255, 0, alpha));
                        g2d.drawLine(x1, y1, x2, y2);
                    }
                    prev = p;
                }
            }
        }

        // Prepare planet rendering info and sort by z-depth
        java.util.List<PlanetRenderInfo> renderList = new java.util.ArrayList<>();
        for (CelestialBody planet : planets) {
            CelestialBody.Point3D pos = planet.getRotatedPosition(rotationX, rotationY);
            int x = cx + (int) (pos.x * scale * zoom / 100);
            int y = cy + (int) (pos.y * scale * zoom / 100);
            int scaledSize = (int)(planet.getDisplaySize() * zoom / 100);
            scaledSize = Math.max(2, Math.min(scaledSize, 150));
            renderList.add(new PlanetRenderInfo(planet, pos, x, y, scaledSize));
        }
        
        // Sort by z-depth: negative z (behind) drawn first, positive z (in front) drawn last
        renderList.sort((a, b) -> Double.compare(a.pos.z, b.pos.z));
        
        // Draw planets behind the Sun (z < 0)
        for (PlanetRenderInfo info : renderList) {
            if (info.pos.z >= 0) break; // Stop when we reach planets in front
            drawPlanet(g2d, info.planet, info.x, info.y, info.pos.z, info.scaledSize);
            if (showLabels) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 11));
                g2d.drawString(info.planet.getName(), info.x + info.scaledSize + 5, info.y + 4);
            }
        }
        
        // Draw Sun in the middle layer
        int sunSize = (int)(25 * zoom / 100);
        sunSize = Math.max(5, Math.min(sunSize, 200));
        drawSun(g2d, cx, cy, sunSize);
        
        // Draw planets in front of the Sun (z >= 0)
        for (PlanetRenderInfo info : renderList) {
            if (info.pos.z < 0) continue; // Skip planets behind
            drawPlanet(g2d, info.planet, info.x, info.y, info.pos.z, info.scaledSize);
            if (showLabels) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 11));
                g2d.drawString(info.planet.getName(), info.x + info.scaledSize + 5, info.y + 4);
            }
        }

        // Draw info overlay (always on top)
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString(String.format("Time: %.2f years", simulationTime), 20, 30);
        g2d.drawString(String.format("Zoom: %.0f%%", zoom), 20, 50);
        
        // Draw selected body info in top right corner
        if (paused && (selectedBody != null || sunSelected)) {
            drawSelectedBodyInfo(g2d);
        }
    }
    
    private void drawSelectedBodyInfo(Graphics2D g2d) {
        int panelWidth = 280;
        int panelX = getWidth() - panelWidth - 20;
        int panelY = 20;
        int lineHeight = 20;
        int padding = 10;
        
        // Semi-transparent background
        g2d.setColor(new Color(0, 0, 0, 180));
        int panelHeight = sunSelected ? 160 : 270;
        g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);
        
        // Border
        g2d.setColor(new Color(100, 150, 255));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);
        
        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String title = sunSelected ? "Sun" : selectedBody.getName();
        g2d.setColor(Color.YELLOW);
        g2d.drawString(title, panelX + padding, panelY + padding + 16);
        
        // Attributes
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));
        int y = panelY + padding + 40;
        
        if (sunSelected) {
            g2d.setColor(Color.WHITE);
            g2d.drawString("Type: Star (G-type)", panelX + padding, y);
            y += lineHeight;
            g2d.drawString("Mass: 1.989 x 10^30 kg", panelX + padding, y);
            y += lineHeight;
            g2d.drawString("Radius: 696,340 km", panelX + padding, y);
            y += lineHeight;
            g2d.drawString("Temperature: 5,778 K", panelX + padding, y);
            y += lineHeight;
            g2d.drawString("Age: 4.6 billion years", panelX + padding, y);
        } else {
            g2d.setColor(Color.WHITE);
            // Display type from C++ polymorphism (Rocky Planet, Gas Giant, etc.)
            String typeStr = selectedBody.getDetailedType();
            g2d.drawString("Type: " + typeStr, panelX + padding, y);
            y += lineHeight;
            
            // Mass
            String massStr;
            if (selectedBody.getMass() >= 1e27) {
                massStr = String.format("%.2f x 10^27 kg", selectedBody.getMass() / 1e27);
            } else if (selectedBody.getMass() >= 1e24) {
                massStr = String.format("%.2f x 10^24 kg", selectedBody.getMass() / 1e24);
            } else {
                massStr = String.format("%.2f x 10^23 kg", selectedBody.getMass() / 1e23);
            }
            g2d.drawString("Mass: " + massStr, panelX + padding, y);
            y += lineHeight;
            
            // Radius
            g2d.drawString(String.format("Radius: %,.0f km", selectedBody.getRadius()), panelX + padding, y);
            y += lineHeight;
            
            // Orbital parameters
            g2d.drawString(String.format("Semi-Major Axis: %.2f AU", selectedBody.getSemiMajorAxis()), panelX + padding, y);
            y += lineHeight;
            g2d.drawString(String.format("Eccentricity: %.3f", selectedBody.getEccentricity()), panelX + padding, y);
            y += lineHeight;
            g2d.drawString(String.format("Orbital Period: %.2f years", selectedBody.getOrbitalPeriod()), panelX + padding, y);
            y += lineHeight;
            g2d.drawString(String.format("Inclination: %.1f degrees", selectedBody.getInclination()), panelX + padding, y);
            y += lineHeight;
            
            // Current distance from Sun
            double dist = Math.sqrt(selectedBody.getX() * selectedBody.getX() + 
                                  selectedBody.getY() * selectedBody.getY() + 
                                  selectedBody.getZ() * selectedBody.getZ());
            g2d.setColor(new Color(180, 255, 180));
            g2d.drawString(String.format("Current Distance: %.3f AU", dist), panelX + padding, y);
            y += lineHeight;
            
            // Surface Gravity (calculated by C++ friend function)
            g2d.setColor(new Color(255, 200, 100));
            double gravity = selectedBody.getSurfaceGravity();
            
            // Compare to Earth's gravity (9.81 m/s²)
            double earthGs = gravity / 9.81;
            g2d.drawString(String.format("Surface Gravity: %.2f m/s²", gravity), panelX + padding, y);
            y += lineHeight;
            g2d.setColor(new Color(200, 200, 200));
            g2d.drawString(String.format("  (%.2fx Earth gravity)", earthGs), panelX + padding, y);
        }
        
        // Instruction at bottom
        g2d.setFont(new Font("Arial", Font.ITALIC, 11));
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawString("Click elsewhere to deselect", panelX + padding, panelY + panelHeight - padding);
    }
    
    private double calculateSpeed(CelestialBody body) {
        if (body.getTrail().size() < 2) return 0.0;
        
        // Get last two points from trail
        int size = body.getTrail().size();
        CelestialBody.Point3D p1 = body.getTrail().get(size - 2);
        CelestialBody.Point3D p2 = body.getTrail().get(size - 1);
        
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        double dz = p2.z - p1.z;
        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
        
        // Speed = distance / time (TIME_STEP per frame)
        return distance / TIME_STEP;
    }
}
