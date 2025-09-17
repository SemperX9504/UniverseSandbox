import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class SolarSystemPanel extends JPanel {
    private static final int WIDTH = 1000, HEIGHT = 750;
    private static final int TIMER_DELAY = 16;
    private static final double G = 1;
    private static final double VELOCITY_SCALE = 0.05;

    private final List<CelestialBody> bodies;
    private final CelestialBody sun;
    private final CelestialBody def;

    private int[] dragStartPoint;
    private int[] currentDragPoint;

    public SolarSystemPanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        bodies = new ArrayList<>();

        double sunMass = 10000;
        int sunRadius = 40;
        sun = new CelestialBody(sunMass, WIDTH / 2.0, HEIGHT / 2.0, 0, 0, sunRadius, Color.YELLOW);
        bodies.add(sun);


        def = new CelestialBody(150, WIDTH / 5.0, HEIGHT * 0.8, Color.BLUE);
        bodies.add(def);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPoint = new int[]{e.getX(), e.getY()};
                currentDragPoint = new int[]{e.getX(), e.getY()};
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                currentDragPoint = new int[]{e.getX(), e.getY()};
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragStartPoint == null) return;

                try {
                    String massStr = JOptionPane.showInputDialog(SolarSystemPanel.this, "Enter planet mass (e.g., 50):", "New Planet", JOptionPane.PLAIN_MESSAGE);
                    if (massStr == null) {
                        resetDrag();
                        return;
                    }
                    double mass = Double.parseDouble(massStr);

                    double vx = (e.getX() - dragStartPoint[0]) * VELOCITY_SCALE;
                    double vy = (e.getY() - dragStartPoint[1]) * VELOCITY_SCALE;

                    int planetRadius = 10;
                    CelestialBody newPlanet = new CelestialBody(mass, dragStartPoint[0], dragStartPoint[1], vx, vy, planetRadius, Color.WHITE);
                    bodies.add(newPlanet);

                    resetDrag();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(SolarSystemPanel.this, "Invalid input. Please enter a number.", "Error", JOptionPane.ERROR_MESSAGE);
                    resetDrag();
                }
            }

            private void resetDrag() {
                dragStartPoint = null;
                currentDragPoint = null;
                repaint();
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        Timer timer = new Timer(TIMER_DELAY, e -> {
            updateSystem();
            repaint();
        });
        timer.start();
    }

    private void updateSystem() {
        for (CelestialBody body : bodies) {
            if (body == sun) continue;

            double dx = sun.x - body.x;
            double dy = sun.y - body.y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < 1) continue;

            double force = (G * sun.mass * body.mass) / (distance * distance);
            double forceX = force * (dx / distance);
            double forceY = force * (dy / distance);
            double ax = forceX / body.mass;
            double ay = forceY / body.mass;

            body.vx += ax;
            body.vy += ay;
            body.updatePosition(1.0);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (CelestialBody body : bodies) {
            g.setColor(body.color);
            int drawX = (int) (body.x - body.radius);
            int drawY = (int) (body.y - body.radius);
            g.fillOval(drawX, drawY, body.radius * 2, body.radius * 2);
        }

        if (dragStartPoint != null && currentDragPoint != null) {
            g.setColor(Color.CYAN);
            g.drawLine(dragStartPoint[0], dragStartPoint[1], currentDragPoint[0], currentDragPoint[1]);
        }
    }
}