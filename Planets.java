

public class Planets {
    private String name;
    private double mass;
    private double radius;
    private double dissun;
    private double ovel;
    private double rperiod;
    private double temperature;

    public Planets(String name,double mass,double radius,double dissun,double ovel,double rperiod,double temperature){
        this.name = name;
        this.mass = mass;
        this.radius = radius;
        this.dissun = dissun;
        this.ovel = ovel;
        this.rperiod = rperiod;
        this.temperature = temperature;
    }

    public void displayinfo(){
        System.out.println("Planet" + name);
        System.out.println("mass"+ mass);
        System.out.println("radius"+ radius);
        System.out.println("distance from sun"+ dissun);
        System.out.println("orbital velocity"+ ovel);
        System.out.println("rotation period"+ rperiod);
        System.out.println("temperature"+ temperature);

    }

    public static void main(String[] args){
        System.out.println("simple class");
        System.out.println("example earth");

        Planets mercury = new Planets("Mercury", 3.285e23, 2.439e6, 5.79e10, 47400.0, 1407.6, 167.0);
        Planets venus   = new Planets("Venus",   4.867e24, 6.052e6, 1.082e11, 35020.0, -5832.5, 464.0);
        Planets earth   = new Planets("Earth",   5.972e24, 6.371e6, 1.496e11, 29780.0, 24.0, 15.0);
        Planets mars    = new Planets("Mars",    6.39e23,  3.389e6, 2.279e11, 24070.0, 24.6, -65.0);
        Planets jupiter = new Planets("Jupiter", 1.898e27, 6.9911e7, 7.785e11, 13070.0, 9.9, -110.0);
        Planets saturn  = new Planets("Saturn",  5.683e26, 5.8232e7, 1.433e12, 9690.0, 10.7, -140.0);
        Planets uranus  = new Planets("Uranus",  8.681e25, 2.5362e7, 2.872e12, 6810.0, -17.2, -195.0);
        Planets neptune = new Planets("Neptune", 1.024e26, 2.4622e7, 4.495e12, 5430.0, 16.1, -200.0);
        Planets pluto   = new Planets("Pluto",   1.309e22, 1.188e6, 5.906e12, 4748.0, 153.3, -225.0);

    }
}