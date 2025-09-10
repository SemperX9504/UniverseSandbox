public class Planets {
    private String name;
    private double mass;
    private double radius;
    private vector2D dissun;
    private vector2D ovel;
    private double rperiod;
    private double temperature;

    public Planets(String name,double mass,double radius,
                   vector2D dissun,vector2D ovel,double rperiod,double temperature){
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
        System.out.println("distance from sun"+ dissun.magnitude());
        System.out.println("orbital velocity"+ ovel.magnitude());
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
            new vector2D(1.496e11,0.0),
            new vector2D(0.0,29780.0),
            24.0,
            15.0
        );
        earth.displayinfo();
    }
}