#ifndef __LED_SYSTEM__
#define __LED_SYSTEM__

#include "Led.hpp"

class TwoLedMonitor {
   public:
    enum class State { AVAILABLE, UNAVAILABLE };
    TwoLedMonitor(Led* green_led, Led* red_led);
    void setState(State new_state);

   private:
    Led* green_led;
    Led* red_led;
};

#endif