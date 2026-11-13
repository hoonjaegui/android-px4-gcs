#include "warning_policy.h"

DroneWarningState evaluate(const DroneState& state, std::int64_t nowMs) {
    DroneWarningState w;
    w.connectionLost = state.connection == ConnectionStatus::Lost;
    w.gpsUnavailable = state.gpsAvailable == false;
    w.lowBattery = state.batteryPercent.has_value() && *state.batteryPercent < kLowBatteryPercent;

    for (const auto& [field, receivedAt] : state.receivedAtMs) {
        if (nowMs - receivedAt > kStaleAfterMs) {
            w.staleFields.insert(field);
        }
    }

    return w;
}