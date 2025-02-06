#include "Led.hpp"
#include "Arduino.h"

Led::Led(int pin) {
    this->pin = pin;
    pinMode(pin, OUTPUT);
    this->state = State::OFF;
}

void Led::setState(State new_state) {
    this->state = new_state;
    digitalWrite(pin, (int)state);
}

Led::State Led::getState() { return state; };