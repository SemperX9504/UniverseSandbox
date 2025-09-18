package src;

import java.awt.*;
import javax.swing.*;

public class Planet {
    String name, info;
    int diameter;
    double angle, speed, orbitRadius;
    Image image;

    public Planet(String name, int diameter, double orbitRadius, double speed,
                  String imgPath, String info) {
        this.name = name;
        this.diameter = diameter;
        this.orbitRadius = orbitRadius;
        this.speed = speed;
        this.info = info;
        this.angle = Math.random() * 360;

        // Load and scale image
        ImageIcon icon = new ImageIcon(imgPath);
        this.image = icon.getImage().getScaledInstance(diameter, diameter, Image.SCALE_SMOOTH);
    }

    public void updatePosition() {
        angle += speed;
        if (angle >= 360) angle = 0;
    }

    public void draw(Graphics g, int cx, int cy) {
        int x = cx + (int)(orbitRadius * Math.cos(Math.toRadians(angle))) - diameter / 2;
        int y = cy + (int)(orbitRadius * Math.sin(Math.toRadians(angle))) - diameter / 2;
        g.drawImage(image, x, y, null);

        g.setColor(Color.WHITE);
        g.drawString(name, x + diameter / 2, y + diameter + 12);
    }

    public boolean isClicked(int mx, int my, int cx, int cy) {
        int x = cx + (int)(orbitRadius * Math.cos(Math.toRadians(angle))) - diameter / 2;
        int y = cy + (int)(orbitRadius * Math.sin(Math.toRadians(angle))) - diameter / 2;
        double dist = Math.sqrt(Math.pow(mx - (x + diameter / 2), 2) + Math.pow(my - (y + diameter / 2), 2));
        return dist <= diameter / 2;
    }
}
