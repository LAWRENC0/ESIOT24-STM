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

State* state;
ServoMotorImpl* servo;
int servo_door_angle, servo_door_update, servo_door_temp, serial_door_angle;
float temp;
Potentiometer* potentiometer;
UserConsole* lcd;
ButtonImpl* button;
String receivedMsg = "";

void switchState() {
    state->switchValue();
    MsgService.sendMsg("{\"window_state\": \"" + state->toString() + "\"}");
    // MsgService.sendMsg(state->toString());
}

void setup() {
    Serial.begin(BAUD_RATE);
    MsgService.init(BAUD_RATE);
    state = new State();
    servo = new ServoMotorImpl(PIN_SERVO, TICK_SPEED_MS, MOTOR_OPEN);
    potentiometer = new Potentiometer(PIN_POT);
    lcd = new UserConsole();
    lcd->init();
    lcd->test();
    button = new ButtonImpl(PIN_BUTTON);
    enableInterrupt(PIN_BUTTON, switchState, RISING);
    servo_door_update = 0;

    servo_door_angle = 0;
    temp = 0;
}

void wait(unsigned long time) {
    unsigned long ts = millis();
    for (; millis() - ts < time;);
}

void printToScreen() {
    lcd->clearScreen();
    lcd->display("Door: " + String(servo_door_angle), 0);
    lcd->display(state->toString(), 1);
    if (state->getValue() == State::Value::MANUAL) {
        lcd->display("Temp: " + String(temp), 2);
    }
}

void loop() {
    disableInterrupt(PIN_BUTTON);
    printToScreen();
    servo_door_update = 0;
    if (state->getValue() == State::Value::MANUAL) {
        servo_door_temp = map(potentiometer->getValue(), 0, 1023, MOTOR_CLOSE, MOTOR_OPEN);
        if (servo_door_temp != servo_door_angle) {
            servo_door_angle = servo_door_temp;
            servo_door_update = 1;
            MsgService.sendMsg("{\"angle\": " + (String)servo_door_angle + "}");
        }
    }
    Msg* msg = MsgService.receiveMsg();
    if (msg != NULL) {
        receivedMsg = msg->getContent();
        if (Pattern::matchTemp(receivedMsg)) {
            temp = Pattern::getTemp(receivedMsg);
        } else if (Pattern::matchAngle(receivedMsg)) {
            if (servo_door_update == 0) {
                servo_door_angle = Pattern::getAngle(receivedMsg);
            }
        } else if (Pattern::matchState(receivedMsg)) {
            if (Pattern::getState(receivedMsg) == "automatic") {
                state->setValue(State::Value::AUTOMATIC);
            } else if (Pattern::getState(receivedMsg) == "manual") {
                state->setValue(State::Value::MANUAL);
            }
        }
        delete msg;
    }

    // MOVE DOOR
    servo->moveToPosition(servo_door_angle);
    enableInterrupt(PIN_BUTTON, switchState, RISING);
    // DISPLAY ON LCD
    wait(TICK_SPEED_MS);
}