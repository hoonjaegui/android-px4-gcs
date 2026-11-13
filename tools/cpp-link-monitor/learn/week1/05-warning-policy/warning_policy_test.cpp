#include <cassert>
#include <iostream>

#include "warning_policy.h"

void searchingIsNotConnectionLoss() {
    DroneState state;
    state.connection = ConnectionStatus::Searching;
    assert(!evaluate(state, 0).connectionLost);
}

void lostAfterConnectedIsConnectionLoss() {
    DroneState state;
    state.connection = ConnectionStatus::Lost;
    assert(evaluate(state, 0).connectionLost);
}

void gpsUnknownIsNotGpsLost() {
    DroneState state;
    state.connection = ConnectionStatus::Connected;
    assert(!evaluate(state, 0).gpsUnavailable);

    state.gpsAvailable = false;
    assert(evaluate(state, 0).gpsUnavailable);
}

bool isLow(std::optional<int> percent) {
    DroneState state;
    state.connection = ConnectionStatus::Connected;
    state.batteryPercent = percent;
    return evaluate(state, 0).lowBattery;
}

void lowBatteryBelowThreshold() {
    assert(!isLow(std::nullopt));
    assert(!isLow(kLowBatteryPercent));
    assert(isLow(kLowBatteryPercent - 1));
}

const std::set<TelemetryField> kOnlyPosition{TelemetryField::Position};

void fieldBecomeStaleOnlyAfterThreshold() {
    DroneState state;
    state.connection = ConnectionStatus::Connected;
    state.receivedAtMs[TelemetryField::Position] = 1000;

    assert(evaluate(state, 1000 + kStaleAfterMs).staleFields.empty());
    assert(evaluate(state, 1001 + kStaleAfterMs).staleFields == kOnlyPosition);
}

void neverReceivedFieldIsNotStale() {
    DroneState state;
    state.connection = ConnectionStatus::Connected;
    assert(evaluate(state, 1'000'000).staleFields.empty());
}

void staleDataWhileDisconnectedIsStillStale() {
    DroneState state;
    state.connection = ConnectionStatus::Lost;
    state.receivedAtMs[TelemetryField::Position] = 0;

    DroneWarningState w = evaluate(state, 10'000);
    assert(w.connectionLost);
    assert(w.staleFields == kOnlyPosition);
}

int main() {
    searchingIsNotConnectionLoss();
    lostAfterConnectedIsConnectionLoss();
    gpsUnknownIsNotGpsLost();
    lowBatteryBelowThreshold();

    fieldBecomeStaleOnlyAfterThreshold();
    neverReceivedFieldIsNotStale();
    staleDataWhileDisconnectedIsStillStale();

    std::cout << "all tests passed\n";
    return 0;
}