import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class solarSystem extends JFrame {

    // Sun object
    class Sun {
        int x, y, diameter;
        String info;

        Sun(int x, int y, int diameter, String info) {
            this.x = x;
            this.y = y;
            this.diameter = diameter;
            this.info = info;
        }

        void draw(Graphics g) {
            g.setColor(Color.YELLOW);
            g.fillOval(x, y, diameter, diameter);
            g.setColor(Color.WHITE);
            g.drawString("Sun", x + diameter / 4, y + diameter / 2);
        }

        boolean isClicked(int mx, int my) {
            double dist = Math.sqrt(Math.pow(mx - (x + diameter / 2), 2) + Math.pow(my - (y + diameter / 2), 2));
            return dist <= diameter / 2;
        }
    }

    // Earth object
    class Earth {
        int x, y, diameter;
        String info;

        Earth(int x, int y, int diameter, String info) {
            this.x = x;
            this.y = y;
            this.diameter = diameter;
            this.info = info;
        }

        void draw(Graphics g) {
            g.setColor(Color.BLUE);
            g.fillOval(x, y, diameter, diameter);
            g.setColor(Color.WHITE);
            g.drawString("Earth", x + diameter / 4, y + diameter / 2);
        }

        boolean isClicked(int mx, int my) {
            double dist = Math.sqrt(Math.pow(mx - (x + diameter / 2), 2) + Math.pow(my - (y + diameter / 2), 2));
            return dist <= diameter / 2;
        }
    }

    private Sun sun;
    private Earth earth;

    public solarSystem() {
        setTitle("Solar System");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // sun and earth properties
        sun = new Sun(150, 150, 100, "Sun Properties:\nWeight: 1.989 × 10^30 kg\nRadius: 696,340 km\nTemperature: 5778 K");
        earth = new Earth(300, 170, 40, "Earth Properties:\nWeight: 5.972 × 10^24 kg\nRadius: 6,371 km\nTemperature: 15°C avg");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();

                if (sun.isClicked(mx, my)) {
                    JOptionPane.showMessageDialog(solarSystem.this, sun.info, "Sun Info", JOptionPane.INFORMATION_MESSAGE);
                } else if (earth.isClicked(mx, my)) {
                    JOptionPane.showMessageDialog(solarSystem.this, earth.info, "Earth Info", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        sun.draw(g);
        earth.draw(g);
    }

    public static void main(String[] args) {
        solarSystem frame = new solarSystem();
        frame.setVisible(true);
    }
}
