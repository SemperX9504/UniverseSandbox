#include "SolarSystemSimulation.cpp"
#include <iostream>
#include <unistd.h>
#include <arpa/inet.h>
#include <cstring>
using namespace std;

int main() {
    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_port = htons(5050);
    addr.sin_addr.s_addr = INADDR_ANY;

    if (::bind(server_fd, (sockaddr*)&addr, sizeof(addr)) < 0) {
        perror("bind failed");
        return 1;
    }

    listen(server_fd, 1);
    cout << "C++ physics server running on port 5050\n";

    sockaddr_in clientAddr{};
    socklen_t clientLen = sizeof(clientAddr);
    int client = accept(server_fd, (sockaddr*)&clientAddr, &clientLen);
    cout << "Java client connected!\n";

    SolarSystemSimulation* sim = new SolarSystemSimulation();
    char buffer[256];

    while (true) {
        ssize_t bytes = read(client, buffer, sizeof(buffer) - 1);
        if (bytes <= 0) break;

        buffer[bytes] = '\0';
        
        // Check for RESET command
        if (strncmp(buffer, "RESET", 5) == 0) {
            delete sim;
            sim = new SolarSystemSimulation();
            
            // Send initial positions after reset
            string json = sim->update(0.0);
            json += "\n";
            send(client, json.c_str(), json.size(), 0);
            continue;
        }
        
        // Otherwise parse as timestep
        double dt;
        if (sscanf(buffer, "%lf", &dt) != 1) continue;

        string json = sim->update(dt);
        json += "\n";
        send(client, json.c_str(), json.size(), 0);
    }

    delete sim;

    close(client);
    close(server_fd);
    return 0;
}
