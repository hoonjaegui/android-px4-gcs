#pragma once
#include <optional>
#include <cstdint>      
#include <map>          
#include <set>          

enum class ConnectionStatus { Searching, Connected, Lost };

enum class TelemetryField { Position, Altitude, Heading, Battery, Gps };

struct DroneState {
    ConnectionStatus connection = ConnectionStatus::Searching;
    std::optional<int> batteryPercent;
    std::optional<bool> gpsAvailable;
    std::map<TelemetryField, std::int64_t> receivedAtMs;
};

struct DroneWarningState {
    bool connectionLost = false;
    bool gpsUnavailable = false;
    bool lowBattery = false;
    std::set<TelemetryField> staleFields;
};

constexpr int kLowBatteryPercent = 20;
constexpr std::int64_t kStaleAfterMs = 3000;

DroneWarningState evaluate(const DroneState& state, std::int64_t nowMs);