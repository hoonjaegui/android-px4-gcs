#include <iostream>

void drain(int p) {
    p = p -10;
    std::cout << " inside drain " << p << '\n';
}

void drainRef(int& p) {
    p = p - 10;
    std::cout << " insdide drainRef " << p << '\n';
}

int main() {
    int battery = 50;

    drain(battery);
    std::cout << " after drain: " << battery << '\n';

    drainRef(battery);
    std::cout << " after drainRef:" << battery << '\n';

    return 0;
}