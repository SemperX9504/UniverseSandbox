

public class Planets {
    private String name;
    private double mass;
    private double radius;
    private double dissun;
    private double ovel;
    private double rperiod;
    private double temperature;

    public Planets(String name,double mass,double radius,
                   double dissun,double ovel,double rperiod,double temperature){
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

        Planets earth = new Planets(
            "Earth",
            5.972e24,
            6.371e6,
            1.496e11,
            29780.0,
            24.0,
            15.0
        );
        earth.displayinfo();
    }
}