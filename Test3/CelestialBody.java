package Test3;

import java.awt.*;
import java.util.LinkedList;

public class CelestialBody {
    private String name;
    private Color color;
    private Color secondaryColor; // For gradients
    private double x, y, z;
    private int displaySize; // Visual size in pixels
    private boolean hasRings; // For Saturn
    private final LinkedList<Point3D> trail = new LinkedList<>();
    private static final int MAX_TRAIL_LENGTH = 150;
    
    // Scientific data
    private double mass; // in kg
    private double radius; // in km
    private double semiMajorAxis; // in AU
    private double eccentricity;
    private double orbitalPeriod; // in Earth years
    private double inclination; // in degrees
    private double surfaceGravity; // calculated by C++ friend function
    private String detailedType; // from C++ polymorphic getType() - "Rocky Planet", "Gas Giant", etc.

    public CelestialBody(String name, Color color, Color secondaryColor, int displaySize, boolean hasRings,
                         double mass, double radius, double semiMajorAxis, double eccentricity, 
                         double orbitalPeriod, double inclination) {
        this.name = name;
        this.color = color;
        this.secondaryColor = secondaryColor;
        this.displaySize = displaySize;
        this.hasRings = hasRings;
        this.mass = mass;
        this.radius = radius;
        this.semiMajorAxis = semiMajorAxis;
        this.eccentricity = eccentricity;
        this.orbitalPeriod = orbitalPeriod;
        this.inclination = inclination;
    }
    
    public CelestialBody(String name, Color color, int displaySize) {
        this(name, color, color.brighter(), displaySize, false, 0, 0, 0, 0, 0, 0);
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;

        // Add new trail point
        trail.add(new Point3D(x, y, z));
        if (trail.size() > MAX_TRAIL_LENGTH)
            trail.removeFirst();
    }
    
    public void setPosition(double x, double y, double z, double gravity) {
        setPosition(x, y, z);
        this.surfaceGravity = gravity;
    }
    
    public void setPosition(double x, double y, double z, double gravity, String type) {
        setPosition(x, y, z, gravity);
        this.detailedType = type;
    }

    public void reset() {
        this.x = this.y = this.z = 0;
        trail.clear();
    }

    public LinkedList<Point3D> getTrail() {
        return trail;
    }

    public Point3D getRotatedPosition(double rotX, double rotY) {
        double y1 = y * Math.cos(rotX) - z * Math.sin(rotX);
        double z1 = y * Math.sin(rotX) + z * Math.cos(rotX);
        double x2 = x * Math.cos(rotY) + z1 * Math.sin(rotY);
        double z2 = -x * Math.sin(rotY) + z1 * Math.cos(rotY);
        return new Point3D(x2, y1, z2);
    }

    public static LinkedList<Point3D> getRotatedTrail(LinkedList<Point3D> trail, double rotX, double rotY) {
        LinkedList<Point3D> rotated = new LinkedList<>();
        for (Point3D p : trail) {
            double y1 = p.y * Math.cos(rotX) - p.z * Math.sin(rotX);
            double z1 = p.y * Math.sin(rotX) + p.z * Math.cos(rotX);
            double x2 = p.x * Math.cos(rotY) + z1 * Math.sin(rotY);
            double z2 = -p.x * Math.sin(rotY) + z1 * Math.cos(rotY);
            rotated.add(new Point3D(x2, y1, z2));
        }
        return rotated;
    }

    public String getName() { return name; }
    public Color getColor() { return color; }
    public Color getSecondaryColor() { return secondaryColor; }
    public int getDisplaySize() { return displaySize; }
    public boolean hasRings() { return hasRings; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getMass() { return mass; }
    public double getRadius() { return radius; }
    public double getSemiMajorAxis() { return semiMajorAxis; }
    public double getEccentricity() { return eccentricity; }
    public double getOrbitalPeriod() { return orbitalPeriod; }
    public double getInclination() { return inclination; }
    public double getSurfaceGravity() { return surfaceGravity; }
    public String getDetailedType() { return detailedType != null ? detailedType : "Planet"; }

    public static class Point3D {
        public final double x, y, z;
        public Point3D(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }
    }
}
