import java.awt.*;

public class CelestialBody {
    // Physical properties
    private String name;
    private double mass;            // kg
    private double radius;          // km
    private Color color;
    private Color trailColor;
    
    // Orbital properties
    private double semiMajorAxis;   // AU (Astronomical Units)
    private double eccentricity;    // Orbital eccentricity (0-1)
    private double orbitalPeriod;   // Earth years
    private double inclination;     // Orbital inclination in radians
    private double argumentOfPeriapsis; // Argument of periapsis in radians
    private double longitudeOfAscendingNode; // Longitude of ascending node in radians
    
    // Current state
    private double currentAngle;    // Current position in orbit (radians)
    private double x, y, z;        // 3D position in AU
    private double vx, vy, vz;     // Velocity components
    private double time;           // Current time in simulation (Earth years)
    
    // Orbital trail
    private java.util.List<Point3D> trail;
    private static final int MAX_TRAIL_LENGTH = 200;
    
    // Constants
    public static final double AU = 149597870.7;     // Astronomical Unit in km
    public static final double G = 6.67430e-11;      // Gravitational constant
    public static final double SOLAR_MASS = 1.989e30; // Solar mass in kg
    
    public CelestialBody(String name, double mass, double radius, Color color,
                  double semiMajorAxis, double eccentricity, double orbitalPeriod,
                  double inclination) {
        this(name, mass, radius, color, semiMajorAxis, eccentricity, orbitalPeriod,
             inclination, 0, 0);
    }
    
    public CelestialBody(String name, double mass, double radius, Color color,
                  double semiMajorAxis, double eccentricity, double orbitalPeriod,
                  double inclination, double argumentOfPeriapsis, double longitudeOfAscendingNode) {
        this.name = name;
        this.mass = mass;
        this.radius = radius;
        this.color = color;
        this.trailColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
        this.semiMajorAxis = semiMajorAxis;
        this.eccentricity = eccentricity;
        this.orbitalPeriod = orbitalPeriod;
        this.inclination = inclination;
        this.argumentOfPeriapsis = argumentOfPeriapsis;
        this.longitudeOfAscendingNode = longitudeOfAscendingNode;
        
        this.currentAngle = Math.random() * 2 * Math.PI;
        this.time = 0;
        this.trail = new java.util.ArrayList<>();
        
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
        time += deltaTime * speedMultiplier;
        double meanMotion = 2 * Math.PI / orbitalPeriod;
        currentAngle += meanMotion * deltaTime * speedMultiplier;
        currentAngle = currentAngle % (2 * Math.PI);
        if (currentAngle < 0) currentAngle += 2 * Math.PI;
        updatePosition();
        updateTrail();
    }
    
    private void updatePosition() {
        Point3D pos = calculatePositionAtAngle(currentAngle);
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }
    
    private Point3D calculatePositionAtAngle(double angle) {
        double r = semiMajorAxis * (1 - eccentricity * eccentricity) / 
                   (1 + eccentricity * Math.cos(angle));
        
        double xOrb = r * Math.cos(angle + argumentOfPeriapsis);
        double yOrb = r * Math.sin(angle + argumentOfPeriapsis);
        double zOrb = 0;
        
        double cosI = Math.cos(inclination);
        double sinI = Math.sin(inclination);
        double cosO = Math.cos(longitudeOfAscendingNode);
        double sinO = Math.sin(longitudeOfAscendingNode);
        
        double x1 = cosO * xOrb - sinO * yOrb;
        double y1 = sinO * xOrb + cosO * yOrb;
        double z1 = zOrb;
        
        double x2 = x1;
        double y2 = cosI * y1 - sinI * z1;
        double z2 = sinI * y1 + cosI * z1;
        
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
    
    public java.util.List<Point3D> getRotatedTrail(double rotX, double rotY, double rotZ) {
        java.util.List<Point3D> rotatedTrail = new java.util.ArrayList<>();
        for (Point3D point : trail) {
            rotatedTrail.add(rotatePoint(point, rotX, rotY, rotZ));
        }
        return rotatedTrail;
    }
    
    private Point3D rotatePoint(Point3D point, double rotX, double rotY, double rotZ) {
        double x1 = point.x;
        double y1 = point.y * Math.cos(rotX) - point.z * Math.sin(rotX);
        double z1 = point.y * Math.sin(rotX) + point.z * Math.cos(rotX);
        
        double x2 = x1 * Math.cos(rotY) + z1 * Math.sin(rotY);
        double y2 = y1;
        double z2 = -x1 * Math.sin(rotY) + z1 * Math.cos(rotY);
        
        double x3 = x2 * Math.cos(rotZ) - y2 * Math.sin(rotZ);
        double y3 = x2 * Math.sin(rotZ) + y2 * Math.cos(rotZ);
        double z3 = z2;
        
        return new Point3D(x3, y3, z3);
    }
    
    public Point3D calculateGravitationalForce(CelestialBody other) {
        double dx = other.x - this.x;
        double dy = other.y - this.y;
        double dz = other.z - this.z;
        
        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (distance == 0) return new Point3D(0, 0, 0);
        
        double force = G * this.mass * other.mass / (distance * distance);
        double forceX = force * dx / distance;
        double forceY = force * dy / distance;
        double forceZ = force * dz / distance;
        
        return new Point3D(forceX, forceY, forceZ);
    }
    
    // Getters
    public String getName() { return name; }
    public double getMass() { return mass; }
    public double getRadius() { return radius; }
    public Color getColor() { return color; }
    public Color getTrailColor() { return trailColor; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getSemiMajorAxis() { return semiMajorAxis; }
    public double getEccentricity() { return eccentricity; }
    public double getOrbitalPeriod() { return orbitalPeriod; }
    public double getInclination() { return inclination; }
    public double getCurrentAngle() { return currentAngle; }
    public double getTime() { return time; }
    public java.util.List<Point3D> getTrail() { return trail; }
    
    public void setPosition(double x, double y, double z) {
        this.x = x; this.y = y; this.z = z;
    }
    
    public void setVelocity(double vx, double vy, double vz) {
        this.vx = vx; this.vy = vy; this.vz = vz;
    }
    
    public static class Point3D {
        public final double x, y, z;
        
        public Point3D(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }
        
        public Point3D add(Point3D other) {
            return new Point3D(x + other.x, y + other.y, z + other.z);
        }
        
        public Point3D multiply(double scalar) {
            return new Point3D(x * scalar, y * scalar, z * scalar);
        }
        
        public double magnitude() {
            return Math.sqrt(x*x + y*y + z*z);
        }
        
        @Override
        public String toString() {
            return String.format("(%.3f, %.3f, %.3f)", x, y, z);
        }
    }
}
