#include "kernel/Pattern.hpp"

#include "kernel/MsgService.hpp"

boolean Pattern::matchTemp(const String& msg) {
    if (!msg.startsWith(F("temperature:"))) {
        return false;
    }
    String valueStr = msg.substring(12, msg.length());
    float tempValue = valueStr.toFloat();
    return tempValue != 0.0 || valueStr == F("0") || valueStr.indexOf('.') != -1;
}

boolean Pattern::matchAngle(const String& msg) {
    if (!msg.startsWith(F("angle:"))) {
        return false;
    }
    String valueStr = msg.substring(6, msg.length());
    int angleValue = valueStr.toInt();
    if (angleValue == 0 && valueStr != F("0")) {
        return false;
    }
    return true;
}

boolean Pattern::matchState(const String& msg) {
    if (msg.startsWith(F("window_state:automatic")) || msg.startsWith(F("window_state:manual"))) return true;
    return false;
}

float Pattern::getTemp(const String& msg) { return msg.substring(12, msg.length()).toFloat(); }

int Pattern::getAngle(const String& msg) { return msg.substring(6, msg.length()).toInt(); }

String Pattern::getState(const String& msg) {
    if (msg.startsWith(F("window_state:automatic"))) return F("automatic");
    if (msg.startsWith(F("window_state:manual"))) return F("manual");
}
