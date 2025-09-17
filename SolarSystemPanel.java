import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;

public class SolarSystemPanel extends JPanel{
    private static final int WIDTH = 1000, HEIGHT = 750;
    private static final int TIMER_DELAY = 8;
    private static final double G = 6.674e-1;
    private static final double VELOCITY_SCALE = 0.05;

    private final List<CelestialBody> bodies;
    private final List<Explosion> explosions;

    private vector2D dragStartPoint;
    private vector2D currentDragPoint;

    private static class Explosion{
        vector2D position;
        double currentRadius;
        double maxRadius;
        int age;
        final int lifetime = 30;
        Color color;

        Explosion(vector2D position, double startRadius, Color color1, Color color2){
            this.position = position;
            this.currentRadius = startRadius;
            this.maxRadius = startRadius * 4;
            this.age = 0;

            this.color = new Color(
                   (color1.getRed() + color2.getRed()) / 2,
                   (color1.getGreen() + color2.getGreen()) / 2,
                   (color1.getBlue() + color2.getBlue()) / 2
            );
        }

        void update(){
            age++;
            currentRadius +=(maxRadius - currentRadius) * 0.1;
        }

        boolean isFinished(){
            return age >= lifetime;
        }

        void draw(Graphics g){
            float alpha = 1.0f -((float) age / lifetime);
            alpha = Math.max(0, Math.min(1, alpha));

            Graphics2D g2d =(Graphics2D) g;
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),(int)(alpha * 255)));
            int r =(int) currentRadius;
            g2d.fillOval((int)(position.x - r),(int)(position.y - r), r * 2, r * 2);
        }
    }

    public SolarSystemPanel(){
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        bodies = new ArrayList<>();
        explosions = new ArrayList<>();

        double sunMass = 10000;
        int sunRadius = 40;
        CelestialBody sun = new CelestialBody(sunMass, WIDTH / 2.0, HEIGHT / 2.0, 0, 0, sunRadius, Color.YELLOW);
        bodies.add(sun);
        CelestialBody def = new CelestialBody(150, WIDTH / 5.0, HEIGHT * 0.8, Color.BLUE);
        bodies.add(def);

        MouseAdapter mouseHandler = new MouseAdapter(){
            @Override
            public void mousePressed(MouseEvent e){
                dragStartPoint = new vector2D(e.getX(), e.getY());
                currentDragPoint = new vector2D(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e){
                currentDragPoint = new vector2D(e.getX(), e.getY());
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e){
                if(dragStartPoint == null) return;

                try{
                    String massStr = JOptionPane.showInputDialog(SolarSystemPanel.this, "Enter planet mass(e.g., 50):", "New Planet", JOptionPane.PLAIN_MESSAGE);
                    if(massStr == null){
                        resetDrag();
                        return;
                    }
                    double mass = Double.parseDouble(massStr);

                    vector2D releasePoint = new vector2D(e.getX(), e.getY());
                    vector2D velocity = releasePoint.diff(dragStartPoint).constProduct(VELOCITY_SCALE);

                    int planetRadius =(int) Math.max(5, Math.pow(mass, 1.0/3.0) * 1.5);
                    CelestialBody newPlanet = new CelestialBody(mass, dragStartPoint, velocity, planetRadius, Color.WHITE);
                    bodies.add(newPlanet);

                    resetDrag();
                } catch(NumberFormatException ex){
                    JOptionPane.showMessageDialog(SolarSystemPanel.this, "Invalid input. Please enter a number.", "Error", JOptionPane.ERROR_MESSAGE);
                    resetDrag();
                }
            }

            private void resetDrag(){
                dragStartPoint = null;
                currentDragPoint = null;
                repaint();
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        Timer timer = new Timer(TIMER_DELAY, e ->{
            updateSystem();
            repaint();
        });
        timer.start();
    }

    private void updateSystem(){
        Map<CelestialBody, vector2D> forces = new java.util.HashMap<>();
        for(CelestialBody body : bodies){
            forces.put(body, new vector2D(0, 0));
        }
        Set<CelestialBody> collidedBodies = new HashSet<>();

        for(int i = 0; i < bodies.size(); i++){
            for(int j = i + 1; j < bodies.size(); j++){
                CelestialBody body1 = bodies.get(i);
                CelestialBody body2 = bodies.get(j);

                double distance = body1.position.distanceTo(body2.position);
                if(distance < body1.radius + body2.radius){
                    collidedBodies.add(body1);
                    collidedBodies.add(body2);

                    double largerRadius = Math.max(body1.radius, body2.radius);
                    vector2D impactPosition = body1.position.sum(body2.position).constProduct(0.5);
                    explosions.add(new Explosion(impactPosition, largerRadius, body1.color, body2.color));
                    continue;
                }

                vector2D r = body2.position.diff(body1.position);
                double distSq = Math.max(r.dotProduct(r), 100);
                double forceMag =(G * body1.mass * body2.mass) / distSq;
                vector2D forceVec = r.unitVector().constProduct(forceMag);

                forces.put(body1, forces.get(body1).sum(forceVec));
                forces.put(body2, forces.get(body2).diff(forceVec));
            }
        }

        bodies.removeAll(collidedBodies);
        for(CelestialBody body : bodies){
            if(body.mass > 5000) continue;
            vector2D force = forces.get(body);
            vector2D acceleration = force.constProduct(1.0 / body.mass);
            body.velocity = body.velocity.sum(acceleration.constProduct(1.0));
            body.updatePosition(1.0);
        }

        for(Iterator<Explosion> it = explosions.iterator(); it.hasNext();){
            Explosion ex = it.next();
            ex.update();
            if(ex.isFinished()){
                it.remove();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2d =(Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for(CelestialBody body : bodies){
            g.setColor(body.color);
            int drawX =(int)(body.position.x - body.radius);
            int drawY =(int)(body.position.y - body.radius);
            g.fillOval(drawX, drawY, body.radius * 2, body.radius * 2);
        }

        for(Explosion ex : explosions){
            ex.draw(g);
        }

        if(dragStartPoint != null && currentDragPoint != null){
            g.setColor(Color.CYAN);
            g.drawLine((int)dragStartPoint.x,(int)dragStartPoint.y,(int)currentDragPoint.x,(int)currentDragPoint.y);
        }
    }
}
