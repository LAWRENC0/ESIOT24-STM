#include "Arduino.h"
#include "kernel/MsgService.hpp"

String content;

MsgServiceClass MsgService;

bool MsgServiceClass::isMsgAvailable() { return msgAvailable; }

Msg* MsgServiceClass::receiveMsg() {
    if (msgAvailable) {
        Msg* msg = currentMsg;
        msgAvailable = false;
        currentMsg = NULL;
        content = "";
        return msg;
    } else {
        return NULL;
    }
}

void MsgServiceClass::init(int baud_rate) {
    Serial.begin(baud_rate);
    content.reserve(128);
    content = "";
    currentMsg = NULL;
    msgAvailable = false;
}

void MsgServiceClass::sendMsg(const String& msg) {
    if (Serial.availableForWrite() > 0) {
        Serial.println(msg);
        Serial.flush();
    }
}

void serialEvent() {
    /* reading the content */
    while (Serial.available()) {
        char ch = (char)Serial.read();
        if (ch == '\n') {
            MsgService.currentMsg = new Msg(content);
            MsgService.msgAvailable = true;
        } else {
            content += ch;
        }
    }
}

bool MsgServiceClass::checkStartMessage(const String& start_message) {
    if (Serial.available() > 0) {
        String received = Serial.readString();
        received.trim();

        if (received == start_message) {
            return true;
        }
    }
    return false;
}