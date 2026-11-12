#pragma once
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

DroneWarningState evaluate(const DroneState& state);