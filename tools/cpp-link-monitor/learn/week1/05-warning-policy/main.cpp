#include <iostream>
#include "warning_policy.h"

void print(const char* label, const DroneWarningState& w) {
    std::cout << std::boolalpha << label
            << ": lost=" << w.connectionLost
            << " gpsUnavailable=" << w.gpsUnavailable
            << " lowBattery=" << w.lowBattery << "\n";
}

int main() {
    DroneState initial;
    print("initial", evaluate(initial));

    DroneState lost;
    lost.connection = ConnectionStatus::Lost;
    lost.batteryPercent = 15;
    print("lost", evaluate(lost));

    DroneState noGps;
    noGps.connection = ConnectionStatus::Connected;
    noGps.batteryPercent = 50;
    noGps.gpsAvailable = false;
    print("noGps", evaluate(noGps));

    return 0;
}