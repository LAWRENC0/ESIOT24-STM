#include <WiFi.h>
#include <PubSubClient.h>
#include "config.hpp"
#include "TwoLedMonitor.hpp"
#include "Led.hpp"
#include "TempSensor.hpp"

const char* ssid = "REDACTED_SSID";
const char* password = "REDACTED_PASSWORD";
const char* mqtt_server = "broker.mqtt-dashboard.com";
const char* temp_topic = "LAWRENC0-STM/temperature";
const char* frequency_topic = "LAWRENC0-STM/frequency";

unsigned long lastMsgTime = 0;
char msg[MSG_BUFFER_SIZE];
long int value = 0;

WiFiClient espClient;
PubSubClient client(espClient);
float network_state;

TaskHandle_t NetworkTask;

Led* red_led;
Led* green_led;
TwoLedMonitor* leds;
TempSensor* temp_sensor;
long frequency_ms;
float temperature;

void setup_wifi() {
  delay(10);
  Serial.println(String("Connecting to ") + ssid);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.println(String("Message arrived on [") + topic + "] len: " + length);
}

void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Create a random client ID
    String clientId = String("LAWRENC0-STM-client-") + String(random(0xffff), HEX);
    // Attempt to connect
    if (client.connect(clientId.c_str())) {
      Serial.println("connected");
      // Once connected, publish an announcement...
      // client.publish("outTopic", "hello world");
      // ... and resubscribe
      client.subscribe(temp_topic);
      client.subscribe(frequency_topic);
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      vTaskDelay(500 / portTICK_PERIOD_MS);
    }
  }
}

void setup() {
  Serial.begin(115200);

  setup_wifi();
  randomSeed(micros());
  client.setServer(mqtt_server, MQTT_PORT);
  client.setCallback(callback);

  pinMode(PIN_GREEN_LED, OUTPUT);
  green_led = new Led(PIN_GREEN_LED);
  pinMode(PIN_RED_LED, OUTPUT);
  red_led = new Led(PIN_RED_LED);
  leds = new TwoLedMonitor(green_led, red_led);
  temp_sensor = new TempSensor(PIN_TEMP_SENSOR);

  network_state = true;
  temperature = temp_sensor->detect();
  frequency_ms = 10000;

  xTaskCreatePinnedToCore(NetworkTaskCode, "NetworkTask", 10000, NULL, 1, &NetworkTask, 0);
  delay(500);  // ???
}

void NetworkTaskCode(void* parameter) {
  for (;;) {
    if (!client.connected()) {
      network_state = false;
      reconnect();
    }
    client.loop();

    unsigned long now = millis();
    if (now - lastMsgTime > 10000) {
      lastMsgTime = now;
      value++;

      /* creating a msg in the buffer */
      snprintf(msg, MSG_BUFFER_SIZE, "{temp: %f}", temperature);
      Serial.println(String("Publishing message: ") + msg);
      client.publish(temp_topic, msg);

      
      snprintf(msg, MSG_BUFFER_SIZE, "{frequency: %ld}", frequency_ms);
      Serial.println(String("Publishing message: ") + msg);
      client.publish(frequency_topic, msg);
    }
    vTaskDelay(500 / portTICK_PERIOD_MS);
  }
}

void loop() {
  vTaskDelay(frequency_ms / portTICK_PERIOD_MS);
  temperature = temp_sensor->detect();
  if(network_state == 1){
    leds->setState(TwoLedMonitor::State::AVAILABLE);
  }else{
    leds->setState(TwoLedMonitor::State::UNAVAILABLE);
  }
}
