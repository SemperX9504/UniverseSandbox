public class Physics {
    private static final double G = 6.67430e-11;
    private static final double mass_sun = 1.989e30;

    public static double cgravity(double mass, double radius) {
        return (G * mass) / (radius * radius);
    }

    public static double getevelo(double mass, double radius) {
        return Math.sqrt((2 * G * mass) / radius);
    }

    public static double getovelo(double dissun) {
        return Math.sqrt((G * mass_sun) / dissun);
    }

    public static double getdensity(double mass, double radius) {
        double volume = (4.0 / 3.0) * Math.PI * Math.pow(radius, 3);
        return mass / volume;
    }

    public static double changemass(double mass, double factor) {
        return mass * factor;
    }

    public static double changeradius(double radius, double factor) {
        return radius * factor;
    }

    public static double simulatechange(double mass, double radius, double massfactor) {
        double newmass = changemass(mass, massfactor);
        return cgravity(newmass, radius);
    }

    public static double simulatechange(double mass, double radius, double radiusfactor, boolean changeradius) {
        double newradius = changeradius(radius, radiusfactor);
        return cgravity(mass, newradius);
    }

    public static double simulatechangedensity(double mass, double radius, double densityfactor) {
        double olddensity = getdensity(mass, radius);
        double newdensity = olddensity * densityfactor;
        double volume = mass / newdensity;
        double newradius = Math.cbrt((3 * volume) / (4 * Math.PI));
        return cgravity(mass, newradius);
    }
}