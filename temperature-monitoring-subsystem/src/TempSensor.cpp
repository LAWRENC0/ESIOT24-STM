#include "TempSensor.hpp"
#include <Arduino.h>

TempSensor::TempSensor(int pin) {
  this->pin = pin;
}

float TempSensor::detect() {
  int adcValue = analogRead(pin);

  float voltage = (adcValue * 3.3) / 4095.0;
  float temperatureC = (voltage - 0.5) * 100;
  return temperatureC;
}