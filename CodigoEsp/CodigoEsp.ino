// =============================================================
// EcoWinds ESP32 firmware — comunicação via MQTT
//
// Subscreve ecowinds/ac/command para receber ON/OFF do backend.
// Publica telemetria, heartbeat e logs de volta.
//
// Temperatura: campo preparado. Para habilitar, defina
// HAS_TEMP_SENSOR em config.h e conecte um DHT22 no pino
// TEMP_SENSOR_PIN.
// =============================================================

#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <time.h>

#include <IRremoteESP8266.h>
#include <IRsend.h>
#include <ir_Fujitsu.h>

#include "config.h"

#ifdef HAS_TEMP_SENSOR
#include <DHT.h>
DHT dht(TEMP_SENSOR_PIN, DHT22);
#endif

// ---- Clientes ----
WiFiClient   wifiClient;
PubSubClient mqtt(wifiClient);
IRFujitsuAC  ac(IR_SEND_PIN);

// ---- Estado local ----
enum AcState { AC_UNKNOWN, AC_ON, AC_OFF };
AcState localAcState = AC_UNKNOWN;

unsigned long lastHeartbeatAt  = 0;
unsigned long lastTelemetryAt  = 0;
unsigned long lastWifiRetryAt  = 0;
unsigned long lastMqttRetryAt  = 0;

// ---- Protótipos ----
void publishTelemetry();
void publishHeartbeat();
void publishLog(const char* action, const char* detail = nullptr);
void driveAc(AcState desired);

// =============================================================
// Callback MQTT — recebe comandos do backend
// =============================================================
void mqttCallback(char* topic, byte* payload, unsigned int length) {
  String msg;
  for (unsigned int i = 0; i < length; i++) msg += (char)payload[i];
  Serial.printf("[mqtt] recv topic=%s payload=%s\n", topic, msg.c_str());

  if (strcmp(topic, MQTT_TOPIC_COMMAND) != 0) return;

  StaticJsonDocument<128> doc;
  if (deserializeJson(doc, msg) != DeserializationError::Ok) {
    Serial.println("[mqtt] invalid JSON");
    return;
  }

  const char* action = doc["action"] | "";
  if (strcmp(action, "ON") == 0) {
    driveAc(AC_ON);
  } else if (strcmp(action, "OFF") == 0) {
    driveAc(AC_OFF);
  } else {
    Serial.printf("[mqtt] unknown action: %s\n", action);
  }
}

// =============================================================
// Wi-Fi
// =============================================================
void ensureWifi() {
  if (WiFi.status() == WL_CONNECTED) return;
  unsigned long now = millis();
  if (now - lastWifiRetryAt < WIFI_RETRY_INTERVAL_MS) return;
  lastWifiRetryAt = now;

  Serial.println("[wifi] connecting...");
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 15000) {
    delay(250);
    Serial.print(".");
  }
  Serial.println();
  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("[wifi] connected ip=%s\n", WiFi.localIP().toString().c_str());
  } else {
    Serial.println("[wifi] connect failed");
  }
}

// =============================================================
// MQTT
// =============================================================
void ensureMqtt() {
  if (mqtt.connected()) return;
  unsigned long now = millis();
  if (now - lastMqttRetryAt < MQTT_RECONNECT_DELAY_MS) return;
  lastMqttRetryAt = now;

  Serial.printf("[mqtt] connecting to %s:%d ...\n", MQTT_BROKER_HOST, MQTT_BROKER_PORT);
  if (mqtt.connect(MQTT_CLIENT_ID)) {
    Serial.println("[mqtt] connected");
    mqtt.subscribe(MQTT_TOPIC_COMMAND, 1);
    publishLog("BOOT", WiFi.localIP().toString().c_str());
    publishTelemetry();
  } else {
    Serial.printf("[mqtt] failed rc=%d\n", mqtt.state());
  }
}

// =============================================================
// IR actuation
// =============================================================
void driveAc(AcState desired) {
  if (desired == localAcState) return;
  if (desired == AC_ON) {
    ac.on();
    ac.send();
    delay(150);
    ac.send();
    Serial.println("[ir] AC -> ON");
    publishLog("AC_ON");
  } else if (desired == AC_OFF) {
    ac.off();
    ac.send();
    delay(150);
    ac.send();
    Serial.println("[ir] AC -> OFF");
    publishLog("AC_OFF");
  }
  localAcState = desired;
  publishTelemetry();
}

// =============================================================
// Publicações MQTT
// =============================================================
void publishTelemetry() {
  if (!mqtt.connected()) return;

  StaticJsonDocument<128> doc;
  doc["air_on"] = (localAcState == AC_ON);

#ifdef HAS_TEMP_SENSOR
  float temp = dht.readTemperature();
  if (!isnan(temp)) doc["temperature"] = temp;
  else              doc["temperature"] = nullptr;
#else
  doc["temperature"] = nullptr;
#endif

  String payload;
  serializeJson(doc, payload);
  mqtt.publish(MQTT_TOPIC_TELEMETRY, payload.c_str(), true);  // retained
  Serial.printf("[mqtt] telemetry published: %s\n", payload.c_str());
}

void publishHeartbeat() {
  if (!mqtt.connected()) return;

  StaticJsonDocument<96> doc;
  doc["rssi"]   = WiFi.RSSI();
  doc["uptime"] = millis() / 1000;

  String payload;
  serializeJson(doc, payload);
  mqtt.publish(MQTT_TOPIC_HEARTBEAT, payload.c_str());
  Serial.printf("[mqtt] heartbeat: rssi=%d\n", WiFi.RSSI());
}

void publishLog(const char* action, const char* detail) {
  if (!mqtt.connected()) return;

  StaticJsonDocument<128> doc;
  doc["action"] = action;
  if (detail) doc["detail"] = detail;

  String payload;
  serializeJson(doc, payload);
  mqtt.publish(MQTT_TOPIC_LOG, payload.c_str());
  Serial.printf("[mqtt] log: %s\n", action);
}

// =============================================================
// NTP
// =============================================================
void syncTime() {
  configTime(NTP_GMT_OFFSET_S, NTP_DST_OFFSET_S, NTP_SERVER_1, NTP_SERVER_2);
  struct tm timeinfo;
  if (getLocalTime(&timeinfo, 5000)) {
    Serial.printf("[ntp] %04d-%02d-%02d %02d:%02d:%02d\n",
                  timeinfo.tm_year + 1900, timeinfo.tm_mon + 1, timeinfo.tm_mday,
                  timeinfo.tm_hour, timeinfo.tm_min, timeinfo.tm_sec);
  } else {
    Serial.println("[ntp] sync failed");
  }
}

// =============================================================
// Setup / Loop
// =============================================================
void setup() {
  Serial.begin(115200);
  delay(200);
  Serial.println("\n[boot] EcoWinds ESP32 starting (MQTT mode)");

  ac.begin();

#ifdef HAS_TEMP_SENSOR
  dht.begin();
#endif

  mqtt.setServer(MQTT_BROKER_HOST, MQTT_BROKER_PORT);
  mqtt.setCallback(mqttCallback);
  mqtt.setKeepAlive(60);

  ensureWifi();
  if (WiFi.status() == WL_CONNECTED) {
    syncTime();
    ensureMqtt();
  }
}

void loop() {
  ensureWifi();

  if (WiFi.status() == WL_CONNECTED) {
    ensureMqtt();
    mqtt.loop();

    unsigned long now = millis();

    if (now - lastHeartbeatAt >= HEARTBEAT_INTERVAL_MS) {
      lastHeartbeatAt = now;
      publishHeartbeat();
    }

    if (now - lastTelemetryAt >= TELEMETRY_INTERVAL_MS) {
      lastTelemetryAt = now;
      publishTelemetry();
    }
  }

  delay(100);
}
