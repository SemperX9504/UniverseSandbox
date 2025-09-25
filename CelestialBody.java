import java.awt.*;
import java.util.*;
import java.util.List;

public class CelestialBody {
    // Physical properties
    private String name;
    private double mass;            // kg
    private double radius;          // km
    private Color color;
    
    private double semiMajorAxis;   // AU
    private double eccentricity;
    private double orbitalPeriod;   // Earth years
    private double inclination;     // radians
    
    private double currentAngle;
    private double x, y, z;         // AU
    private double time;
    
    private List<Point3D> trail;
    private static final int MAX_TRAIL_LENGTH = 200;
    
    public static final double AU = 149597870.7;           // km
    public static final double G = 6.67430e-11;            // m³/kg⋅s²
    public static final double SOLAR_MASS = 1.989e30;      // kg
    
    public CelestialBody(String name, double mass, double radius, Color color, double semiMajorAxis, double eccentricity, double orbitalPeriod, double inclination) {
        this.name = name;
        this.mass = mass;
        this.radius = radius;
        this.color = color;
        this.semiMajorAxis = semiMajorAxis;
        this.eccentricity = eccentricity;
        this.orbitalPeriod = orbitalPeriod;
        this.inclination = Math.toRadians(inclination);
        
        this.currentAngle = Math.random() * 2 * Math.PI;
        this.time = 0;
        this.trail = new ArrayList<>();
        
        updatePosition();
        initializeTrail();
    }
    
    private void initializeTrail() {
        trail.clear();
        double tempAngle = currentAngle;
        for (int i = 0; i < MAX_TRAIL_LENGTH; i++) {
            double angle = tempAngle - (i * 2 * Math.PI / MAX_TRAIL_LENGTH);
            Point3D point = calculatePositionAtAngle(angle);
            trail.add(0, point);
        }
    }
    
    public void updatePosition(double deltaTime, double speedMultiplier) {
        if (semiMajorAxis > 0) {
            time += deltaTime * speedMultiplier;
            double meanMotion = 2 * Math.PI / orbitalPeriod;
            currentAngle += meanMotion * deltaTime * speedMultiplier;
            currentAngle = currentAngle % (2 * Math.PI);
            if (currentAngle < 0) currentAngle += 2 * Math.PI;
            updatePosition();
            updateTrail();
        }
    }
    
    private void updatePosition() {
        Point3D pos = calculatePositionAtAngle(currentAngle);
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }
    
    private Point3D calculatePositionAtAngle(double angle) {
        if (semiMajorAxis == 0) return new Point3D(0, 0, 0);
        
        double r = semiMajorAxis * (1 - eccentricity * eccentricity) / (1 + eccentricity * Math.cos(angle));
        
        double xOrb = r * Math.cos(angle);
        double yOrb = r * Math.sin(angle);
        double zOrb = 0;
        
        double x2 = xOrb;
        double y2 = yOrb * Math.cos(inclination) - zOrb * Math.sin(inclination);
        double z2 = yOrb * Math.sin(inclination) + zOrb * Math.cos(inclination);
        
        return new Point3D(x2, y2, z2);
    }
    
    private void updateTrail() {
        trail.add(new Point3D(x, y, z));
        if (trail.size() > MAX_TRAIL_LENGTH) {
            trail.remove(0);
        }
    }
    
    public void reset() {
        time = 0;
        currentAngle = Math.random() * 2 * Math.PI;
        updatePosition();
        initializeTrail();
    }
    
    public Point3D getRotatedPosition(double rotX, double rotY, double rotZ) {
        return rotatePoint(new Point3D(x, y, z), rotX, rotY, rotZ);
    }
    
    public List<Point3D> getRotatedTrail(double rotX, double rotY, double rotZ) {
        List<Point3D> rotatedTrail = new ArrayList<>();
        for (Point3D point : trail) {
            rotatedTrail.add(rotatePoint(point, rotX, rotY, rotZ));
        }
        return rotatedTrail;
    }
    
    private Point3D rotatePoint(Point3D point, double rotX, double rotY, double rotZ) {
        
        double y1 = point.y * Math.cos(rotX) - point.z * Math.sin(rotX);
        double z1 = point.y * Math.sin(rotX) + point.z * Math.cos(rotX);
        
        double x2 = point.x * Math.cos(rotY) + z1 * Math.sin(rotY);
        double y2 = y1;
        double z2 = -point.x * Math.sin(rotY) + z1 * Math.cos(rotY);
        
        double x3 = x2 * Math.cos(rotZ) - y2 * Math.sin(rotZ);
        double y3 = x2 * Math.sin(rotZ) + y2 * Math.cos(rotZ);
        double z3 = z2;
        
        return new Point3D(x3, y3, z3);
    }
    
    public String getName() { return name; }
    public double getMass() { return mass; }
    public double getRadius() { return radius; }
    public Color getColor() { return color; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getSemiMajorAxis() { return semiMajorAxis; }
    public double getEccentricity() { return eccentricity; }
    public double getOrbitalPeriod() { return orbitalPeriod; }
    public double getInclination() { return inclination; }
    public double getCurrentAngle() { return currentAngle; }
    public double getTime() { return time; }
    public List<Point3D> getTrail() { return trail; }
    
    public static class Point3D {
        public final double x, y, z;
        
        public Point3D(double x, double y, double z) {
            this.x = x; 
            this.y = y; 
            this.z = z;
        }
        
        @Override
        public String toString() {
            return String.format("(%.3f, %.3f, %.3f)", x, y, z);
        }
    }
}
