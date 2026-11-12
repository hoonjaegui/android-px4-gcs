#include <iostream>
#include <optional>

enum class ConnectionStatus { Searching, Connected, Lost };

struct DroneState {
    ConnectionStatus connection = ConnectionStatus::Searching;
    std::optional<int> batteryPercent;
    std::optional<bool> gpsAvailable;
};

struct DroneWarningState {
    bool connectionLost = false;
    bool gpsUnavailable = false;
    bool lowBattery = false;
};

constexpr int kLowBatteryPercent = 20;

DroneWarningState evaluate(const DroneState& state) {
    DroneWarningState w;
    w.connectionLost = state.connection == ConnectionStatus::Lost;
    w.gpsUnavailable = state.gpsAvailable == false;
    w.lowBattery = state.batteryPercent.has_value() && *state.batteryPercent < kLowBatteryPercent;
    return w;
}

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