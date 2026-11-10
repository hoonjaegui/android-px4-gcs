#include <iostream>
#include "battery.h"

int main() {
    std::cout << isLowBattery(50) << '\n';
    std::cout << isLowBattery(10) << '\n';
    return 0;
}