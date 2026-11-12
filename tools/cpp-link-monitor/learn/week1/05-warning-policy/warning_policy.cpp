#include "warning_policy.h"

DroneWarningState evaluate(const DroneState& state) {
    DroneWarningState w;
    w.connectionLost = state.connection == ConnectionStatus::Lost;
    w.gpsUnavailable = state.gpsAvailable == false;
    w.lowBattery = state.batteryPercent.has_value() && *state.batteryPercent < kLowBatteryPercent;
    return w;
}