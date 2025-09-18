package src;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SolarSystem extends JFrame {

    class Planet {
        int diameter;
        String name, info;
        Color color;
        double angle, orbitRadiusX, orbitRadiusY, speed;
        int centerX, centerY; // orbit center

        Planet(String name, int diameter, Color color, String info,
               int centerX, int centerY, double orbitRadiusX, double orbitRadiusY,
               double speed, double initialAngle) {
            this.name = name;
            this.diameter = diameter;
            this.color = color;
            this.info = info;
            this.centerX = centerX;
            this.centerY = centerY;
            this.orbitRadiusX = orbitRadiusX;
            this.orbitRadiusY = orbitRadiusY;
            this.speed = speed;
            this.angle = initialAngle;
        }

        void updatePosition() {
            angle += speed;
            if (angle > 2 * Math.PI) angle -= 2 * Math.PI;
        }

        int getX() {
            return (int) (centerX + orbitRadiusX * Math.cos(angle)) - diameter / 2;
        }

        int getY() {
            return (int) (centerY + orbitRadiusY * Math.sin(angle)) - diameter / 2;
        }

        void draw(Graphics g) {
            int x = getX();
            int y = getY();

            // Orbit path
            g.setColor(Color.DARK_GRAY);
            g.drawOval(centerX - (int) orbitRadiusX, centerY - (int) orbitRadiusY,
                       (int) orbitRadiusX * 2, (int) orbitRadiusY * 2);

            // Planet
            g.setColor(color);
            g.fillOval(x, y, diameter, diameter);

            // Label
            g.setColor(Color.WHITE);
            g.drawString(name, x + diameter / 4, y + diameter / 2);
        }

        boolean isClicked(int mx, int my) {
            int x = getX();
            int y = getY();
            double dist = Math.sqrt(Math.pow(mx - (x + diameter / 2), 2) + Math.pow(my - (y + diameter / 2), 2));
            return dist <= diameter / 2;
        }
    }

    private Planet sun, mercury, venus, earth, moon, mars, jupiter, uranus, neptune;
    private Timer timer;

    class SolarPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // background
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            // planets
            sun.draw(g);
            mercury.draw(g);
            venus.draw(g);
            earth.draw(g);
            mars.draw(g);
            jupiter.draw(g);
            uranus.draw(g);
            neptune.draw(g);
        }
    }

    public SolarSystem() {
        setTitle("Solar System - Elliptical Orbits");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        int centerX = 450;
        int centerY = 300;

        // Planets
        sun = new Planet("Sun", 100, Color.YELLOW, "Sun Info", centerX, centerY, 0, 0, 0, 0);
        mercury = new Planet("Mercury", 20, Color.GRAY, "Mercury Info", centerX, centerY, 120, 100, 0.05, 0);
        venus = new Planet("Venus", 30, new Color(205, 133, 63), "Venus Info", centerX, centerY, 160, 130, 0.03, 1);
        earth = new Planet("Earth", 40, Color.BLUE, "Earth Info", centerX, centerY, 220, 180, 0.02, 2);
        mars = new Planet("Mars", 30, Color.RED, "Mars Info", centerX, centerY, 300, 250, 0.015, 1);
        jupiter = new Planet("Jupiter", 80, Color.ORANGE, "Jupiter Info", centerX, centerY, 380, 300, 0.01, 0);
        uranus = new Planet("Uranus", 60, Color.CYAN, "Uranus Info", centerX, centerY, 480, 350, 0.007, 2);
        neptune = new Planet("Neptune", 55, Color.BLUE.darker(), "Neptune Info", centerX, centerY, 560, 400, 0.005, 3);

        SolarPanel panel = new SolarPanel();
        add(panel);

        // Timer for animation
        timer = new Timer(2, e -> {
            mercury.updatePosition();
            venus.updatePosition();
            earth.updatePosition();
            mars.updatePosition();
            jupiter.updatePosition();
            uranus.updatePosition();
            neptune.updatePosition();
            panel.repaint();
        });
        timer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SolarSystem frame = new SolarSystem();
            frame.setVisible(true);
        });
    }
}
