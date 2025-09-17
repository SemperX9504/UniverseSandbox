import java.awt.Color;

public class CelestialBody{
    public double mass;
    public vector2D position;
    public vector2D velocity;
    public int radius;
    public Color color;

    public CelestialBody(double mass, vector2D position, vector2D velocity, int radius, Color color){
        this.mass = mass;
        this.position = position;
        this.velocity = velocity;
        this.radius = radius;
        this.color = color;
    }

    // Overloaded constructor for backwards compatibility and ease of use
    public CelestialBody(double mass, double x, double y, double vx, double vy, int radius, Color color){
        this(mass, new vector2D(x, y), new vector2D(vx, vy), radius, color);
    }

    // Original overloaded constructor, updated to use the new vector-based main constructor
    public CelestialBody(double mass, double x, double y, Color color){
        this(mass, new vector2D(x, y), new vector2D(8, 1), 20, color);
    }

    /**
     * Updates the body's position based on its velocity and a given time step.
     * @param timeStep The simulation time step.
     */
    public void updatePosition(double timeStep){
        // The new position is calculated by adding the velocity vector (scaled by time)
        // to the old position vector. p_new = p_old + v * dt
        this.position = this.position.sum(this.velocity.constProduct(timeStep));
    }
}
