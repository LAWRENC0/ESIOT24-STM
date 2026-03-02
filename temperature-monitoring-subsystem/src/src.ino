#include <WiFi.h>
#include <PubSubClient.h>
#include "config.hpp"
#include "secrets.h"
#include "TwoLedMonitor.hpp"
#include "Led.hpp"
#include "TempSensor.hpp"

const char* ssid = WIFI_SSID;
const char* password = WIFI_PASSWORD;
const char* mqtt_server = "broker.mqtt-dashboard.com";
const char* temp_topic = "LAWRENC0-STM/temperature";
const char* frequency_topic = "LAWRENC0-STM/frequency";

char msg[MSG_BUFFER_SIZE];
WiFiClient espClient;
PubSubClient client(espClient);
TaskHandle_t NetworkTask;

QueueHandle_t temperatureQueue, frequencyQueueThread, frequencyQueueSLoop, networkStateQueue;

Led* red_led;
Led* green_led;
TwoLedMonitor* leds;
TempSensor* temp_sensor;
bool net_state;
long frequency_tpm;

void setup_wifi() {
  delay(10);
  // Serial.println(String("Connecting to ") + ssid);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    // Serial.print(".");
  }
  // Serial.println("");
  // Serial.println("WiFi connected");
  // Serial.println("IP address: ");
  // Serial.println(WiFi.localIP());
}

void callback(char* topic, byte* payload, unsigned int length) {
  if (strcmp(topic, frequency_topic) == 0) {
    long freq = atol((char*)payload); 
    // Serial.println(String("Received new frequency: ") + freq);
    xQueueSend(frequencyQueueThread, &freq, 0);
    xQueueSend(frequencyQueueSLoop, &freq, 0);
  }
}

void reconnect() {
  while (!client.connected()) {
    // Serial.print("Attempting MQTT connection...");
    String clientId = String("LAWRENC0-STM-client-") + String(random(0xffff), HEX);
    if (client.connect(clientId.c_str())) {
      // Serial.println("Connected to MQTT");
      client.subscribe(temp_topic);
      client.subscribe(frequency_topic);
    } else {
      // Serial.print("Failed, rc=");
      // Serial.print(client.state());
      // Serial.println(" try again in 0.5 seconds");
      vTaskDelay(500 / portTICK_PERIOD_MS);
    }
  }
}

void setup() {
  // Serial.begin(115200);
  setup_wifi();
  randomSeed(micros());
  client.setServer(mqtt_server, MQTT_PORT);
  client.setCallback(callback);

  green_led = new Led(PIN_GREEN_LED);
  red_led = new Led(PIN_RED_LED);
  leds = new TwoLedMonitor(green_led, red_led);
  leds->setState(TwoLedMonitor::State::AVAILABLE);
  temp_sensor = new TempSensor(PIN_TEMP_SENSOR);
  net_state = false;
  frequency_tpm = DEFAULT_FREQUENCY_TPM;

  temperatureQueue = xQueueCreate(10, sizeof(float));
  frequencyQueueThread = xQueueCreate(5, sizeof(long));
  frequencyQueueSLoop = xQueueCreate(5, sizeof(long));
  networkStateQueue = xQueueCreate(5, sizeof(bool));

  if (!temperatureQueue || !frequencyQueueSLoop || !frequencyQueueThread || !networkStateQueue) {
    // Serial.println("Queue creation failed!");
    while (1);
  }

  xTaskCreatePinnedToCore(NetworkTaskCode, "NetworkTask", 10000, NULL, 1, &NetworkTask, 0);
  delay(500);
}

void NetworkTaskCode(void* parameter) {
  float ntc_temp;
  long ntc_freq = DEFAULT_FREQUENCY_TPM;
  bool ntc_net_state = false;

  for (;;) {
    // Handle MQTT connection and publish temperature
    if (!client.connected()) {
      ntc_net_state = false;
      xQueueSend(networkStateQueue, &ntc_net_state, 0);
      reconnect();
      ntc_net_state = true;
      xQueueSend(networkStateQueue, &ntc_net_state, 0);
    }
    client.loop();

    if (xQueueReceive(temperatureQueue, &ntc_temp, 0) == pdTRUE) {
      snprintf(msg, MSG_BUFFER_SIZE, "{temp: %f}", ntc_temp);
      // Serial.println(String("Publishing message: ") + msg);
      client.publish(temp_topic, msg);
    }

    if(xQueueReceive(frequencyQueueThread, &ntc_freq, 0) == pdTRUE){
      // Serial.println(String("Received frquency value: ") + ntc_freq);
    }

    vTaskDelay((60 / ntc_freq) * 1000 / portTICK_PERIOD_MS);
  }
}

void loop() {
  float temp = temp_sensor->detect();

  // Send temperature to the NetworkTask
  xQueueSend(temperatureQueue, &temp, 0);

  // Receive new frequency if available
  if (xQueueReceive(frequencyQueueSLoop, &frequency_tpm, 0) == pdTRUE) {
    // Serial.println(String("Updated frequency: ") + frequency_tpm);
  }

  // Receive network state and update LEDs
  if (xQueueReceive(networkStateQueue, &net_state, 0) == pdTRUE) {
    // Serial.println(String("net_state: ") + net_state);
    leds->setState(net_state ? TwoLedMonitor::State::AVAILABLE : TwoLedMonitor::State::UNAVAILABLE);
  }

  vTaskDelay((60 / frequency_tpm) * 1000 / portTICK_PERIOD_MS);
}
