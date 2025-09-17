import java.util.List;

public class Physics {
    private static final double G = 6.67430e-11;

    public static double cgravity(CelestialBody body) {
        return (G * body.mass) / (body.radius * body.radius);
    }

    public static double getevelo(CelestialBody body) {
        return Math.sqrt((2 * G * body.mass) / body.radius);
    }

    public static double getdensity(CelestialBody body) {
        double volume = (4.0 / 3.0) * Math.PI * Math.pow(body.radius, 3);
        return body.mass / volume;
    }

    public static double changemass(CelestialBody body, double factor) {
        return body.mass * factor;
    }

    public static double changeradius(CelestialBody body, double factor) {
        return body.radius * factor;
    }

    public static double simulatechangeMass(CelestialBody body, double massfactor) {
        double newmass = changemass(body, massfactor);
        return (G * newmass) / (body.radius * body.radius);
    }

    public static double simulatechangeRadius(CelestialBody body, double radiusfactor) {
        double newradius = changeradius(body, radiusfactor);
        return (G * body.mass) / (newradius * newradius);
    }

    public static double simulatechangedensity(CelestialBody body, double densityfactor) {
        double olddensity = getdensity(body);
        double newdensity = olddensity * densityfactor;
        double volume = body.mass / newdensity;
        double newradius = Math.cbrt((3 * volume) / (4 * Math.PI));
        return (G * body.mass) / (newradius * newradius);
    }

    public static vector2D getAcceleration(CelestialBody target, CelestialBody other) {
        vector2D direction = other.position.diff(target.position);
        double dist = direction.magnitude();

        if (dist < 1) return new vector2D(0, 0);
        double forceMag = (G * other.mass) / (dist * dist);
        return direction.unitVector().constProduct(forceMag);
    }

    public static vector2D getNetAcceleration(CelestialBody target, List<CelestialBody> bodies) {
        vector2D netAcc = new vector2D(0, 0);
        for (CelestialBody other : bodies) {
            if (other == target) continue;
            netAcc = netAcc.sum(getAcceleration(target, other));
        }
        return netAcc;
    }
}
