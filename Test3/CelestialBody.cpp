#include <iostream>
#include <cmath>
#include <string>
using namespace std;

class CelestialObject{
public:
    struct Vec3{
        double x, y, z;
        Vec3(double x = 0, double y = 0, double z = 0) : x(x), y(y), z(z){}
        
        Vec3 operator+(const Vec3& other) const{
            return Vec3(x + other.x, y + other.y, z + other.z);
        }
        
        Vec3 operator-(const Vec3& other) const{
            return Vec3(x - other.x, y - other.y, z - other.z);
        }
        
        Vec3 operator*(double scalar) const{
            return Vec3(x * scalar, y * scalar, z * scalar);
        }
        
        double magnitude() const{
            return sqrt(x*x + y*y + z*z);
        }
        
        Vec3 normalized() const{
            double mag = magnitude();
            if (mag > 0) return Vec3(x/mag, y/mag, z/mag);
            return Vec3(0, 0, 0);
        }
    };
    
    Vec3 position;
    Vec3 velocity;
    Vec3 acceleration;
    
    virtual void update(double dt) = 0;
    virtual string getType() const = 0;
    virtual double getMass() const = 0;
    virtual ~CelestialObject() = default;
};

class CelestialBody : public CelestialObject{
protected:
    string name;
    double mass;
    double radius;
    double semiMajorAxis;
    double eccentricity;
    double period;
    double inclination;

public:

    CelestialBody(string name, double mass, double radius, double semiMajorAxis, double eccentricity, double period, double inclinationDeg) 
        : name(std::move(name)), mass(mass), radius(radius), semiMajorAxis(semiMajorAxis), eccentricity(eccentricity), period(period), inclination(inclinationDeg * M_PI / 180.0){

        if (semiMajorAxis > 0.0){
            computeInitialState();
        } else{
            position = Vec3(0, 0, 0);
            velocity = Vec3(0, 0, 0);
            acceleration = Vec3(0, 0, 0);
        }
    }

    virtual ~CelestialBody() = default;

    void update(double dt) override{
        position = position + velocity * dt + acceleration * (0.5 * dt * dt);
    }
    
    string getType() const override{
        return "Body";
    }
    
    double getMass() const override{
        return mass;
    }
    
    void updateVelocity(double dt, const Vec3& newAcceleration){
        velocity = velocity + (acceleration + newAcceleration) * (0.5 * dt);
        acceleration = newAcceleration;
    }

    const string& getName() const{ return name; }
    
    friend double calculateSurfaceGravity(const CelestialBody& body){
        // g = G * M / R²
        double radiusMeters = body.radius * 1000.0; // radius km to m
        const double G = 6.674e-11; // m³/(kgs²)
        return (G * body.mass) / (radiusMeters * radiusMeters);
    }
    
    double getSurfaceGravity() const{
        return calculateSurfaceGravity(*this);
    }

protected:
    void computeInitialState(){
        double r = semiMajorAxis * (1 - eccentricity);
        
        position.x = r * cos(0);
        position.y = r * sin(0) * cos(inclination);
        position.z = r * sin(0) * sin(inclination);
        
        const double G_M_sun = 4.0 * M_PI * M_PI; // Gaussian gravitational constant
        
        double v = sqrt(G_M_sun * (2.0/r - 1.0/semiMajorAxis));
        
        velocity.x = 0;
        velocity.y = v * cos(inclination);
        velocity.z = v * sin(inclination);
        
        acceleration = Vec3(0, 0, 0);
    }
};

class Planet : public CelestialBody{
    string planetType;
public:
    Planet(string name, double mass, double radius, double semiMajorAxis, double eccentricity, double period, double inclinationDeg, string type) 
        : CelestialBody(name, mass, radius, semiMajorAxis, eccentricity, period, inclinationDeg), planetType(type){}
    
    string getType() const override{
        return planetType + " Planet";
    }
    
    string getPlanetType() const{ return planetType; }
};

class Star : public CelestialBody{
public:
    Star(string name, double mass, double radius) 
        : CelestialBody(name, mass, radius, 0.0, 0.0, 0.0, 0.0){}
    
    string getType() const override{
        return "Star";
    }
    
    void update(double dt) override{}
};
