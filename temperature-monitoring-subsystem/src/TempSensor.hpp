#ifndef __TEMP_SENSOR__
#define __TEMP_SENSOR__

class TempSensor {
public:
  TempSensor(int pin);
  float detect();
private:
  int pin;
};

#endif