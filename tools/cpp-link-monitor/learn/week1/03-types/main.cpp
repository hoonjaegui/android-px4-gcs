#include <iostream>
#include <string>

int main() {
    const int lowThresholde = 20;
    int percent = 19.9;
    std::string mode = "HOLD";
    bool armed = false;
    int unknown;

    std::cout << "percnet:: " << percent <<'\n';
    std::cout << "low:: " << (percent < lowThresholde) << '\n';
    std::cout << "mode:: " << mode << '\n';
    std::cout << "armed:: " << armed <<'\n';
    std::cout << "unknown:: " << unknown << '\n';

    return 0;
}