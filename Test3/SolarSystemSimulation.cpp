#include <string>
#include <vector>
#include <sstream>
#include "CelestialBody.cpp"
using namespace std;

class SolarSystemSimulation {
public:
    SolarSystemSimulation();
    ~SolarSystemSimulation();
    string update(double dt);

private:
    vector<CelestialObject*> bodies;
    double time;
    static constexpr double G = 4.0 * M_PI * M_PI;
    
    CelestialObject::Vec3 computeAcceleration(size_t bodyIndex);
};

SolarSystemSimulation::SolarSystemSimulation() : time(0) {
    bodies.push_back(new Star("Sun", 1.989e30, 696340));
    
    bodies.push_back(new Planet("Mercury", 3.3e23, 2439.7, 0.39, 0.206, 0.24, 7.0, "Rocky"));
    bodies.push_back(new Planet("Venus", 4.87e24, 6051.8, 0.72, 0.007, 0.62, 3.4, "Rocky"));
    bodies.push_back(new Planet("Earth", 5.97e24, 6371, 1.0, 0.017, 1.0, 0.0, "Rocky"));
    bodies.push_back(new Planet("Mars", 6.42e23, 3389.5, 1.52, 0.093, 1.88, 1.9, "Rocky"));
    bodies.push_back(new Planet("Jupiter", 1.898e27, 69911, 5.20, 0.049, 11.86, 1.3, "Gas Giant"));
    bodies.push_back(new Planet("Saturn", 5.68e26, 58232, 9.58, 0.056, 29.46, 2.5, "Gas Giant"));
    bodies.push_back(new Planet("Uranus", 8.68e25, 25362, 19.22, 0.046, 84.01, 0.8, "Ice Giant"));
    bodies.push_back(new Planet("Neptune", 1.02e26, 24622, 30.05, 0.010, 164.8, 1.8, "Ice Giant"));
    
    for (size_t i = 0; i < bodies.size(); ++i) {
        bodies[i]->acceleration = computeAcceleration(i);
    }
}

SolarSystemSimulation::~SolarSystemSimulation() {
    for (auto body : bodies) {
        delete body;
    }
    bodies.clear();
}

CelestialObject::Vec3 SolarSystemSimulation::computeAcceleration(size_t bodyIndex) {
    CelestialObject::Vec3 totalAccel(0, 0, 0);
    CelestialObject* body = bodies[bodyIndex];
    
    for (size_t j = 0; j < bodies.size(); ++j) {
        if (j == bodyIndex) continue;
        
        CelestialObject* other = bodies[j];
        CelestialObject::Vec3 direction = other->position - body->position;
        double distance = direction.magnitude();
        
        if (distance < 1e-10) continue;
        
        // F = G * m1 * m2 / r^2
        // a = F/m = G * m_other / r^2
        double massRatio = other->getMass() / bodies[0]->getMass();
        double accelMagnitude = G * massRatio / (distance * distance);
        
        CelestialObject::Vec3 accel = direction.normalized() * accelMagnitude;
        totalAccel = totalAccel + accel;
    }
    
    return totalAccel;
}

string SolarSystemSimulation::update(double dt) {
    for (size_t i = 0; i < bodies.size(); ++i) {
        bodies[i]->update(dt);
    }
    
    vector<CelestialObject::Vec3> newAccelerations;
    for (size_t i = 0; i < bodies.size(); ++i) {
        newAccelerations.push_back(computeAcceleration(i));
    }
    
    for (size_t i = 0; i < bodies.size(); ++i) {
        CelestialBody* body = dynamic_cast<CelestialBody*>(bodies[i]);
        if (body) {
            body->updateVelocity(dt, newAccelerations[i]);
        }
    }
    
    time += dt;
    
    ostringstream out;
    out << "{\"planets\":[";
    for (size_t i = 1; i < bodies.size(); ++i) {
        auto pos = bodies[i]->position;
        string type = bodies[i]->getType();
        
        CelestialBody* body = dynamic_cast<CelestialBody*>(bodies[i]);
        double gravity = body ? body->getSurfaceGravity() : 0.0;
        
        out << "{\"pos\":[" << pos.x << "," << pos.y << "," << pos.z << "],"
            << "\"gravity\":" << gravity << ","
            << "\"type\":\"" << type << "\"}";
        if (i < bodies.size() - 1) out << ",";
    }
    out << "]}";
    return out.str();
}