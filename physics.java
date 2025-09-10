public class Physics {
    private static final double G = 6.67430e-11; 
    private static final double MASS_SUN = 1.989e30; 

    public static double getGravity(double mass, double radius) {
        return (G * mass) / (radius * radius);
    }

    public static double getEscapeVelocity(double mass, double radius) {
        return Math.sqrt((2 * G * mass) / radius);
    }

    public static double getOrbitalVelocity(double distanceFromSun) {
        return Math.sqrt((G * MASS_SUN) / distanceFromSun);
    }

    public static double getDensity(double mass, double radius) {
        double volume = (4.0 / 3.0) * Math.PI * Math.pow(radius, 3);
        return mass / volume;
    }

    public static double changeMass(double mass, double factor) {
        return mass * factor;
    }

    public static double changeRadius(double radius, double factor) {
        return radius * factor;
    }

    public static void simulateChange(double mass, double radius, String property, double factor) {
        double newMass = mass;
        double newRadius = radius;

        switch (property.toLowerCase()) {
            case "mass":
                newMass = changeMass(mass, factor);
                break;
            case "radius":
                newRadius = changeRadius(radius, factor);
                break;
            case "density":
                double oldDensity = getDensity(mass, radius);
                double newDensity = oldDensity * factor;
                double volume = mass / newDensity;
                newRadius = Math.cbrt((3 * volume) / (4 * Math.PI));
                break;
            default:
                System.out.println("Unknown property!");
                return;
        }
        System.out.printf(" New Gravity: %.2f m/s²%n", getGravity(newMass, newRadius));
    }
}
