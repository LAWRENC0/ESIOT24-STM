#include "Led.hpp"
#include "Arduino.h"

Led::Led(int pin) {
    this->pin = pin;
    pinMode(pin, OUTPUT);
    this->setState(State::OFF);
}

void Led::setState(State new_state) {
    this->state = new_state;
    digitalWrite(pin, 1-(int)state);
}

Led::State Led::getState() { return state; };