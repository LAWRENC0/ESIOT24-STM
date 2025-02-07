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

char msg[MSG_BUFFER_SIZE];
WiFiClient espClient;
PubSubClient client(espClient);
TaskHandle_t NetworkTask;

// FreeRTOS queues for shared variables
QueueHandle_t temperatureQueue, frequencyQueue, networkStateQueue;

Led* red_led;
Led* green_led;
TwoLedMonitor* leds;
TempSensor* temp_sensor;
bool net_state;
long frequency_ms;

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
  if (strcmp(topic, frequency_topic) == 0) {
    long freq = atol((char*)payload);  // Convert payload to long
    Serial.println(String("Received new frequency: ") + freq);
    xQueueSend(frequencyQueue, &freq, 0);
  }
}

void reconnect() {
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    String clientId = String("LAWRENC0-STM-client-") + String(random(0xffff), HEX);
    if (client.connect(clientId.c_str())) {
      Serial.println("Connected to MQTT");
      client.subscribe(temp_topic);
      client.subscribe(frequency_topic);
    } else {
      Serial.print("Failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
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

  green_led = new Led(PIN_GREEN_LED);
  red_led = new Led(PIN_RED_LED);
  leds = new TwoLedMonitor(green_led, red_led);
  leds->setState(TwoLedMonitor::State::AVAILABLE);
  temp_sensor = new TempSensor(PIN_TEMP_SENSOR);
  net_state = false;
  frequency_ms = DEFAULT_FREQUENCY_MS;

  // Create FreeRTOS queues
  temperatureQueue = xQueueCreate(10, sizeof(float));
  frequencyQueue = xQueueCreate(5, sizeof(long));
  networkStateQueue = xQueueCreate(5, sizeof(bool));

  if (!temperatureQueue || !frequencyQueue || !networkStateQueue) {
    Serial.println("Queue creation failed!");
    while (1);  // Stop execution if queue creation fails
  }

  // Start network task on Core 0
  xTaskCreatePinnedToCore(NetworkTaskCode, "NetworkTask", 10000, NULL, 1, &NetworkTask, 0);
  delay(500);
}

void NetworkTaskCode(void* parameter) {
  float temp;
  long freq = DEFAULT_FREQUENCY_MS;
  bool network_state = false;

  for (;;) {
    // Handle MQTT connection and publish temperature
    if (!client.connected()) {
      network_state = false;
      xQueueSend(networkStateQueue, &network_state, 0);
      reconnect();
      network_state = true;
      xQueueSend(networkStateQueue, &network_state, 0);
    }
    client.loop();

    if (xQueueReceive(temperatureQueue, &temp, 0) == pdTRUE) {
      snprintf(msg, MSG_BUFFER_SIZE, "{temp: %f}", temp);
      Serial.println(String("Publishing message: ") + msg);
      client.publish(temp_topic, msg);
    }

    if(xQueueReceive(frequencyQueue, &freq, 0)){
      Serial.println("Received frquency value");
    }

    vTaskDelay(freq / portTICK_PERIOD_MS);
  }
}

void loop() {
  float temp = temp_sensor->detect();

  // Send temperature to the NetworkTask
  xQueueSend(temperatureQueue, &temp, 0);

  // Receive new frequency if available
  if (xQueueReceive(frequencyQueue, &frequency_ms, 0) == pdTRUE) {
    Serial.println(String("Updated frequency: ") + frequency_ms);
  }

  // Receive network state and update LEDs
  if (xQueueReceive(networkStateQueue, &net_state, 0) == pdTRUE) {
    Serial.println("net_state: " + net_state);
    leds->setState(net_state ? TwoLedMonitor::State::AVAILABLE : TwoLedMonitor::State::UNAVAILABLE);
  }

  vTaskDelay(frequency_ms / portTICK_PERIOD_MS);
}
