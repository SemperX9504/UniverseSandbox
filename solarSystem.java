import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class solarSystem extends JFrame {


    class Planet {
        int x, y, diameter;
        String name, info;
        Color color;

        Planet(String name, int x, int y, int diameter, Color color, String info) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.diameter = diameter;
            this.color = color;
            this.info = info;
        }

        void draw(Graphics g) {
            g.setColor(color);
            g.fillOval(x, y, diameter, diameter);
            g.setColor(Color.WHITE);
            g.drawString(name, x + diameter / 4, y + diameter / 2);
        }

        boolean isClicked(int mx, int my) {
            double dist = Math.sqrt(Math.pow(mx - (x + diameter / 2), 2) + Math.pow(my - (y + diameter / 2), 2));
            return dist <= diameter / 2;
        }
    }


    private Planet sun, mercury, venus, earth, moon, mars, jupiter, uranus, neptune;

    public solarSystem() {
        setTitle("Solar System");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);


        sun = new Planet("Sun", 150, 200, 100, Color.YELLOW,
                "Sun:\nWeight: 1.989 × 10^30 kg\nRadius: 696,340 km\nTemperature: 5778 K");


        mercury = new Planet("Mercury", 280, 230, 20, Color.GRAY,
                "Mercury:\nWeight: 3.285 × 10^23 kg\nRadius: 2,439 km\nTemperature: 167°C avg");


        venus = new Planet("Venus", 330, 220, 30, new Color(205, 133, 63),
                "Venus:\nWeight: 4.867 × 10^24 kg\nRadius: 6,052 km\nTemperature: 464°C avg");


        earth = new Planet("Earth", 400, 210, 40, Color.BLUE,
                "Earth:\nWeight: 5.972 × 10^24 kg\nRadius: 6,371 km\nTemperature: 15°C avg");

        moon = new Planet("Moon", 430, 200, 7, Color.LIGHT_GRAY,
                "Moon:\nWeight: 7.35 × 10^22 kg\nRadius: 1,737 km\nTemperature: −20°C avg");


        mars = new Planet("Mars", 480, 240, 30, Color.RED,
                "Mars:\nWeight: 6.39 × 10^23 kg\nRadius: 3,389 km\nTemperature: −63°C avg");


        jupiter = new Planet("Jupiter", 600, 180, 80, Color.ORANGE,
                "Jupiter:\nWeight: 1.898 × 10^27 kg\nRadius: 69,911 km\nTemperature: −108°C avg");


        uranus = new Planet("Uranus", 720, 250, 60, Color.CYAN,
                "Uranus:\nWeight: 8.681 × 10^25 kg\nRadius: 25,362 km\nTemperature: −195°C avg");


        neptune = new Planet("Neptune", 820, 220, 55, Color.BLUE.darker(),
                "Neptune:\nWeight: 1.024 × 10^26 kg\nRadius: 24,622 km\nTemperature: −200°C avg");


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
    }

    private void showInfo(Planet p) {
        JOptionPane.showMessageDialog(this, p.info, p.name + " Info", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight()); 

        // Draw planets
        sun.draw(g);
        mercury.draw(g);
        venus.draw(g);
        earth.draw(g);
        moon.draw(g);
        mars.draw(g);
        jupiter.draw(g);
        uranus.draw(g);
        neptune.draw(g);
    }

    public static void main(String[] args) {
        solarSystem frame = new solarSystem();
        frame.setVisible(true);
    }
}
