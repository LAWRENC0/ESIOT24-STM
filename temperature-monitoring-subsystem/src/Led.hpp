#ifndef __LED__
#define __LED__

class Led {
   public:
    enum class State { OFF = 0, ON = 1 };
    Led(int pin);
    void setState(State state);
    State getState();

   private:
    State state;
    int pin;
};

#endif