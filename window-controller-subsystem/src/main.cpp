#include <Arduino.h>
#include <EnableInterrupt.h>
#include "config.hpp"
#include "kernel/State.hpp"
#include "kernel/MsgService.hpp"
#include "kernel/Pattern.hpp"
#include "devices/motors/ServoMotorImpl.hpp"
#include "devices/potentiometer/Potentiometer.hpp"
#include "devices/consoles/UserConsole.hpp"
#include "devices/buttons/ButtonImpl.hpp"

State *state_button, *state_network;
ServoMotorImpl* servo;
Potentiometer* potentiometer;
UserConsole* lcd;
ButtonImpl* button;
int angle_pot, angle_netw, angle;
int angle_pot_upd, angle_netw_upd;
float temp;
int loop_counter;
String receivedMsg;

void switchState() {
    static unsigned long last_hit_time = 0;
    unsigned long current_hit_time = millis();
    if (current_hit_time - last_hit_time > BOUNCE_TIME) {
        last_hit_time = current_hit_time;
        state_button->switchValue();
        MsgService.sendMsg("{\"window_state\": \"" + state_button->toString() + "\"}");
        // MsgService.sendMsg(state_button->toString());
    }
}

void setup() {
    Serial.begin(BAUD_RATE);
    MsgService.init(BAUD_RATE);
    state_button = new State();
    state_network = new State();
    servo = new ServoMotorImpl(PIN_SERVO, TICK_SPEED_MS, MOTOR_OPEN);
    potentiometer = new Potentiometer(PIN_POT);
    lcd = new UserConsole();
    lcd->init();
    lcd->test();
    button = new ButtonImpl(PIN_BUTTON);
    enableInterrupt(PIN_BUTTON, switchState, RISING);
    angle_pot_upd = 0;
    angle_netw_upd = 0;
    angle_pot = 0;
    angle_netw = 0;
    angle = 0;
    long ts = millis();
    for (; millis() - ts < 1000;) {
        servo->moveToPosition(MOTOR_OPEN);
        delay(50);
    }
    ts = millis();
    for (; millis() - ts < 1000;) {
        servo->moveToPosition(MOTOR_CLOSE);
        delay(50);
    }
    temp = 0;
    loop_counter = 0;
    receivedMsg.reserve(128);
    receivedMsg = "";
}

void wait(unsigned long time) {
    unsigned long ts = millis();
    for (; millis() - ts < time;);
}

void printToScreen() {
    lcd->clearScreen();
    lcd->display("Door: " + String(angle), 0);
    lcd->display(state_network->toString(), 1);
    if (state_network->getValue() == State::Value::MANUAL) {
        lcd->display("Temp: " + String(temp), 2);
    }
}

void loop() {
    disableInterrupt(PIN_BUTTON);
    state_button->setValue(state_network->getValue());
    enableInterrupt(PIN_BUTTON, switchState, RISING);
    angle_pot_upd = 0;

    Msg* msg = MsgService.receiveMsg();
    if (msg != NULL) {
        receivedMsg = msg->getContent();
        delete msg;
        if (Pattern::matchTemp(receivedMsg)) {
            temp = Pattern::getTemp(receivedMsg);
        } else if (Pattern::matchAngle(receivedMsg)) {
            angle_netw = Pattern::getAngle(receivedMsg);
            if (angle != angle_netw) {
                angle_netw_upd = 10;
            }
        } else if (Pattern::matchState(receivedMsg)) {
            if (Pattern::getState(receivedMsg) == F("automatic")) {
                state_network->setValue(State::Value::AUTOMATIC);
            } else if (Pattern::getState(receivedMsg) == "manual") {
                state_network->setValue(State::Value::MANUAL);
            }
        }
        while (Serial.available()) Serial.read();
    }

    if (state_network->getValue() == State::Value::MANUAL) {
        int angle_pot_temp = map(potentiometer->getValue(), 0, 1023, MOTOR_CLOSE, MOTOR_OPEN);
        if (abs(angle_pot_temp - angle_pot) > 4 && angle_netw_upd == 0) {
            angle_pot = angle_pot_temp;
            angle_pot_upd = 1;
        }
    }

    // MOVE DOOR
    if (angle_netw_upd > 0) {
        angle = angle_netw;
        angle_netw_upd--;
    }
    if (angle_pot_upd == 1 && angle_netw_upd == 0) {
        MsgService.sendMsg("{\"angle\": " + (String)angle_pot + "}");
    }
    if (angle_netw_upd > 0 || angle_pot_upd == 1) {
        servo->moveToPosition(angle);
    }
    loop_counter = (loop_counter + 1) % 5;
    if (loop_counter == 0) printToScreen();
    // DISPLAY ON LCD
    wait(TICK_SPEED_MS);
}