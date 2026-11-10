#include "battery.h"

bool isLowBattery(int percent) {
    return percent < 20;
}