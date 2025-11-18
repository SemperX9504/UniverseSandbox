#include "SolarSystemSimulation.cpp"
#include <iostream>
#include <cstring>

#ifdef _WIN32
    // Windows (Winsock2)
    #include <winsock2.h>
    #include <ws2tcpip.h>
    #pragma comment(lib, "ws2_32.lib")

    #define CLOSESOCKET closesocket
    #define READ(sock, buf, len) recv(sock, buf, len, 0)
#else
    // Linux / macOS (POSIX sockets)
    #include <unistd.h>
    #include <arpa/inet.h>
    #include <sys/socket.h>
    #define CLOSESOCKET close
    #define READ(sock, buf, len) read(sock, buf, len)
#endif

using namespace std;

int main() {
#ifdef _WIN32
    // Initialize Winsock on Windows
    WSADATA wsaData;
    int wsaInit = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (wsaInit != 0) {
        cerr << "WSAStartup failed: " << wsaInit << endl;
        return 1;
    }
#endif

    // ---- Create server socket ----
    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd < 0) {
        perror("socket failed");
        return 1;
    }

    int opt = 1;
#ifdef _WIN32
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, (const char*)&opt, sizeof(opt));
#else
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
#endif

    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_port = htons(5050);
    addr.sin_addr.s_addr = INADDR_ANY;

    if (::bind(server_fd, (sockaddr*)&addr, sizeof(addr)) < 0) {
        perror("bind failed");
        CLOSESOCKET(server_fd);
#ifdef _WIN32
        WSACleanup();
#endif
        return 1;
    }

    listen(server_fd, 1);
    cout << "C++ physics server running on port 5050" << endl;

    sockaddr_in clientAddr{};
#ifdef _WIN32
    int clientLen = sizeof(clientAddr);
#else
    socklen_t clientLen = sizeof(clientAddr);
#endif
    int client = accept(server_fd, (sockaddr*)&clientAddr, &clientLen);
    if (client < 0) {
        perror("accept failed");
        CLOSESOCKET(server_fd);
#ifdef _WIN32
        WSACleanup();
#endif
        return 1;
    }
    cout << "Java client connected!" << endl;

    SolarSystemSimulation* sim = new SolarSystemSimulation();
    char buffer[256];

    while (true) {
        ssize_t bytes = READ(client, buffer, sizeof(buffer) - 1);
        if (bytes <= 0) break;

        buffer[bytes] = '\0';

        // Check for RESET command
        if (strncmp(buffer, "RESET", 5) == 0) {
            delete sim;
            sim = new SolarSystemSimulation();

            string json = sim->update(0.0);
            json += "\n";
            send(client, json.c_str(), (int)json.size(), 0);
            continue;
        }

        // Otherwise parse as timestep
        double dt;
        if (sscanf(buffer, "%lf", &dt) != 1) continue;

        string json = sim->update(dt);
        json += "\n";
        send(client, json.c_str(), (int)json.size(), 0);
    }

    delete sim;

    CLOSESOCKET(client);
    CLOSESOCKET(server_fd);

#ifdef _WIN32
    WSACleanup();
#endif

    return 0;
}