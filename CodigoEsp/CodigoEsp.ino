// =============================================================
// EcoWinds ESP32 firmware — comunicação via HTTP polling
//
// A cada POLL_INTERVAL_MS faz POST /esp-device/sync enviando
// telemetria (estado do ar, temperatura, rssi, uptime) e recebe
// na resposta o comando desejado: {"action":"ON"|"OFF"}.
// Eventos (boot, liga/desliga) vão para POST /esp-device/log.
//
// Temperatura: campo preparado. Para habilitar, defina
// HAS_TEMP_SENSOR em config.h e conecte um DHT22 no pino
// TEMP_SENSOR_PIN.
// =============================================================

#include <WiFi.h>
#include <HTTPClient.h>
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

IRFujitsuAC ac(IR_SEND_PIN);

// ---- Estado local ----
enum AcState { AC_UNKNOWN, AC_ON, AC_OFF };
AcState localAcState = AC_UNKNOWN;

unsigned long lastSyncAt      = 0;
unsigned long lastWifiRetryAt = 0;

// ---- Protótipos ----
void driveAc(AcState desired);
void postLog(const char* action, const char* detail = nullptr);

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
    postLog("AC_ON");
  } else if (desired == AC_OFF) {
    ac.off();
    ac.send();
    delay(150);
    ac.send();
    Serial.println("[ir] AC -> OFF");
    postLog("AC_OFF");
  }
  localAcState = desired;
}

// =============================================================
// HTTP — sync (telemetria + comando na mesma chamada)
// =============================================================
void syncWithBackend() {
  HTTPClient http;
  http.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
  http.setTimeout(HTTP_READ_TIMEOUT_MS);

  if (!http.begin(BACKEND_BASE_URL "/esp-device/sync")) {
    Serial.println("[http] begin failed");
    return;
  }
  http.addHeader("Content-Type", "application/json");

  JsonDocument req;
  req["airOn"]  = (localAcState == AC_ON);
  req["rssi"]   = WiFi.RSSI();
  req["uptime"] = millis() / 1000;
  req["ip"]     = WiFi.localIP().toString();

#ifdef HAS_TEMP_SENSOR
  float temp = dht.readTemperature();
  if (!isnan(temp)) req["temperature"] = temp;
  else              req["temperature"] = nullptr;
#else
  req["temperature"] = nullptr;
#endif

  String body;
  serializeJson(req, body);

  int code = http.POST(body);
  if (code == 200) {
    JsonDocument resp;
    if (deserializeJson(resp, http.getString()) == DeserializationError::Ok) {
      const char* action = resp["action"] | "";
      if (strcmp(action, "ON") == 0) {
        driveAc(AC_ON);
      } else if (strcmp(action, "OFF") == 0) {
        driveAc(AC_OFF);
      } else {
        Serial.printf("[http] unknown action: %s\n", action);
      }
    } else {
      Serial.println("[http] invalid JSON in response");
    }
  } else {
    Serial.printf("[http] sync failed code=%d\n", code);
  }
  http.end();
}

// =============================================================
// HTTP — log de eventos (best-effort)
// =============================================================
void postLog(const char* action, const char* detail) {
  if (WiFi.status() != WL_CONNECTED) return;

  HTTPClient http;
  http.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
  http.setTimeout(HTTP_READ_TIMEOUT_MS);

  if (!http.begin(BACKEND_BASE_URL "/esp-device/log")) return;
  http.addHeader("Content-Type", "application/json");

  JsonDocument doc;
  doc["action"] = action;
  if (detail) doc["detail"] = detail;

  String payload;
  serializeJson(doc, payload);
  int code = http.POST(payload);
  Serial.printf("[http] log %s code=%d\n", action, code);
  http.end();
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
  Serial.println("\n[boot] EcoWinds ESP32 starting (HTTP polling mode)");

  ac.begin();

#ifdef HAS_TEMP_SENSOR
  dht.begin();
#endif

  ensureWifi();
  if (WiFi.status() == WL_CONNECTED) {
    syncTime();
    postLog("BOOT", WiFi.localIP().toString().c_str());
    syncWithBackend();
    lastSyncAt = millis();
  }
}

void loop() {
  ensureWifi();

  if (WiFi.status() == WL_CONNECTED) {
    unsigned long now = millis();
    if (now - lastSyncAt >= POLL_INTERVAL_MS) {
      lastSyncAt = now;
      syncWithBackend();
    }
  }

  delay(50);
}
