package src;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SolarSystem extends JFrame {

    class Planet {
        int diameter;
        String name, info;
        Color color;
        double angle, orbitRadius, speed;
        int centerX, centerY; // orbit center

        Planet(String name, int diameter, Color color, String info,
               int centerX, int centerY, double orbitRadius, double speed, double initialAngle) {
            this.name = name;
            this.diameter = diameter;
            this.color = color;
            this.info = info;
            this.centerX = centerX;
            this.centerY = centerY;
            this.orbitRadius = orbitRadius;
            this.speed = speed;
            this.angle = initialAngle;
        }

        void updatePosition() {
            angle += speed;
            if (angle > 2 * Math.PI) angle -= 2 * Math.PI;
        }

        int getX() {
            return (int) (centerX + orbitRadius * Math.cos(angle)) - diameter / 2;
        }

        int getY() {
            return (int) (centerY + orbitRadius * Math.sin(angle)) - diameter / 2;
        }

        void draw(Graphics g) {
            int x = getX();
            int y = getY();
            g.setColor(color);
            g.fillOval(x, y, diameter, diameter);
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

    public SolarSystem() {
        setTitle("Solar System - Orbit Animation");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        int centerX = 450; // sun center
        int centerY = 300;

        sun = new Planet("Sun", 100, Color.YELLOW,
                "Sun:\nWeight: 1.989 × 10^30 kg\nRadius: 696,340 km\nTemperature: 5778 K",
                centerX, centerY, 0, 0, 0);

        mercury = new Planet("Mercury", 20, Color.GRAY,
                "Mercury:\nWeight: 3.285 × 10^23 kg\nRadius: 2,439 km\nTemperature: 167°C avg",
                centerX, centerY, 120, 0.05, 0);

        venus = new Planet("Venus", 30, new Color(205, 133, 63),
                "Venus:\nWeight: 4.867 × 10^24 kg\nRadius: 6,052 km\nTemperature: 464°C avg",
                centerX, centerY, 160, 0.03, 1);

        earth = new Planet("Earth", 40, Color.BLUE,
                "Earth:\nWeight: 5.972 × 10^24 kg\nRadius: 6,371 km\nTemperature: 15°C avg",
                centerX, centerY, 220, 0.02, 2);

        moon = new Planet("Moon", 10, Color.LIGHT_GRAY,
                "Moon:\nWeight: 7.35 × 10^22 kg\nRadius: 1,737 km\nTemperature: −20°C avg",
                centerX, centerY, 240, 0.1, 0); // orbit near earth for simplicity

        mars = new Planet("Mars", 30, Color.RED,
                "Mars:\nWeight: 6.39 × 10^23 kg\nRadius: 3,389 km\nTemperature: −63°C avg",
                centerX, centerY, 300, 0.015, 1);

        jupiter = new Planet("Jupiter", 80, Color.ORANGE,
                "Jupiter:\nWeight: 1.898 × 10^27 kg\nRadius: 69,911 km\nTemperature: −108°C avg",
                centerX, centerY, 380, 0.01, 0);

        uranus = new Planet("Uranus", 60, Color.CYAN,
                "Uranus:\nWeight: 8.681 × 10^25 kg\nRadius: 25,362 km\nTemperature: −195°C avg",
                centerX, centerY, 480, 0.007, 2);

        neptune = new Planet("Neptune", 55, Color.BLUE.darker(),
                "Neptune:\nWeight: 1.024 × 10^26 kg\nRadius: 24,622 km\nTemperature: −200°C avg",
                centerX, centerY, 560, 0.005, 3);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();

                if (sun.isClicked(mx, my)) showInfo(sun);
                else if (mercury.isClicked(mx, my)) showInfo(mercury);
                else if (venus.isClicked(mx, my)) showInfo(venus);
                else if (earth.isClicked(mx, my)) showInfo(earth);
                else if (moon.isClicked(mx, my)) showInfo(moon);
                else if (mars.isClicked(mx, my)) showInfo(mars);
                else if (jupiter.isClicked(mx, my)) showInfo(jupiter);
                else if (uranus.isClicked(mx, my)) showInfo(uranus);
                else if (neptune.isClicked(mx, my)) showInfo(neptune);
            }
        });

        // Timer for animation
        timer = new Timer(30, e -> {
            mercury.updatePosition();
            venus.updatePosition();
            earth.updatePosition();
            moon.updatePosition();
            mars.updatePosition();
            jupiter.updatePosition();
            uranus.updatePosition();
            neptune.updatePosition();
            repaint();
        });
        timer.start();
    }

    private void showInfo(Planet p) {
        JOptionPane.showMessageDialog(this, p.info, p.name + " Info", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        sun.draw(g);
        mercury.draw(g);
        venus.draw(g);
        earth.draw(g);
        //moon.draw(g);
        mars.draw(g);
        jupiter.draw(g);
        uranus.draw(g);
        neptune.draw(g);
    }

    public static void main(String[] args) {
        SolarSystem frame = new SolarSystem();
        frame.setVisible(true);
    }
}
