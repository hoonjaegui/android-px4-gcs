#include <cassert>
#include <iostream>

#include "warning_policy.h"

void searchingIsNotConnectionLoss() {
    DroneState state;
    state.connection = ConnectionStatus::Searching;
    assert(!evaluate(state).connectionLost);
}

void lostAfterConnectedIsConnectionLoss() {
    DroneState state;
    state.connection = ConnectionStatus::Lost;
    assert(evaluate(state).connectionLost);
}

void gpsUnknownIsNotGpsLost() {
    DroneState state;
    state.connection = ConnectionStatus::Connected;
    assert(!evaluate(state).gpsUnavailable);

    state.gpsAvailable = false;
    assert(evaluate(state).gpsUnavailable);
}

bool isLow(std::optional<int> percent) {
    DroneState state;
    state.connection = ConnectionStatus::Connected;
    state.batteryPercent = percent;
    return evaluate(state).lowBattery;
}

void lowBatteryBelowThreshold() {
    assert(!isLow(std::nullopt));
    assert(!isLow(kLowBatteryPercent));
    assert(isLow(kLowBatteryPercent - 1));
}

int main() {
    searchingIsNotConnectionLoss();
    lostAfterConnectedIsConnectionLoss();
    gpsUnknownIsNotGpsLost();
    lowBatteryBelowThreshold();
    std::cout << "all tests passed\n";
    return 0;
}