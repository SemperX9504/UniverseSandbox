import java.awt.Color;

public class CelestialBody {
    public double mass;
    public double x, y;
    public double vx, vy;
    public int radius;
    public Color color;

    public CelestialBody(double mass, double x, double y, double vx, double vy, int radius, Color color) {
        this.mass = mass;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.color = color;
    }

    public void updatePosition(double timeStep) {
        this.x += this.vx * timeStep;
        this.y += this.vy * timeStep;
    }
}