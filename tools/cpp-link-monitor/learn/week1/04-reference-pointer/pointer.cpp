#include <iostream>

void printBattery(const int* ptr) {
    if (ptr == nullptr) {
        std::cout << "battery unknown\n";
        return;
    }
    std::cout << "battery: " << *ptr << '\n';
}

int main() {
    int battery = 50;
    int* ptr = &battery;

    std::cout << "address: " << ptr << '\n';
    std::cout << "value: "  << *ptr << '\n';

    *ptr = 30;
    std::cout << "battery after *ptr = 30: " << battery << '\n';

    printBattery(&battery);
    printBattery(nullptr);

    return 0;
}

