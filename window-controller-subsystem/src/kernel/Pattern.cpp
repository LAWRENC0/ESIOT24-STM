#include "kernel/Pattern.hpp"

boolean Pattern::matchTemp(const String& msg) {
    if (!msg.startsWith("{temp: ") || !msg.endsWith("}")) {
        return false;
    }
    String valueStr = msg.substring(7, msg.length() - 1);
    float tempValue = valueStr.toFloat();
    return tempValue != 0.0 || valueStr == "0" || valueStr.indexOf('.') != -1;
}

boolean Pattern::matchAngle(const String& msg) {
    if (!msg.startsWith("{angle: ") || !msg.endsWith("}")) {
        return false;
    }
    String valueStr = msg.substring(8, msg.length() - 1);
    int angleValue = valueStr.toInt();
    if (angleValue == 0 && valueStr != "0") {
        return false;
    }
    return true;
}

boolean Pattern::matchState(const String& msg) {
    if (msg == "{window_state: automatic}" || msg == "{window_state: manual}") return true;
    return false;
}

float Pattern::getTemp(const String& msg) { return msg.substring(7, msg.length() - 1).toFloat(); }

int Pattern::getAngle(const String& msg) { return msg.substring(8, msg.length() - 1).toInt(); }

String Pattern::getState(const String& msg) {
    if (msg == "{window_state: automatic}") return "automatic";
    if (msg == "{window_state: manual}") return "manual";
}
