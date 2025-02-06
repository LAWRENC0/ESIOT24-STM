#include "TwoLedMonitor.hpp"

TwoLedMonitor::TwoLedMonitor(Led* green_led, Led* red_led) {
    this->green_led = green_led;
    this->red_led = red_led;
}

void TwoLedMonitor::setState(State new_state) {
    switch (new_state) {
        case State::AVAILABLE:
            green_led->setState(Led::State::ON);
            red_led->setState(Led::State::OFF);
            break;
        case State::UNAVAILABLE:
            green_led->setState(Led::State::OFF);
            red_led->setState(Led::State::ON);
            break;
        default:
            break;
    }
}