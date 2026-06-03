// =============================================================
// EcoWinds ESP32 firmware
//
// Polls the backend at /api/devices/me/state, drives the Fujitsu AC
// via IR according to the server-computed desired state (respecting
// class schedules and holidays), sends heartbeats and execution logs.
//
// Auth: X-Device-Key header (raw key provisioned by the admin via
// POST /esp-device/{id}/api-key).
//
// Resilience:
//  - Wi-Fi auto-reconnect.
//  - Exponential backoff on API errors.
//  - Last desired state cached in RAM; on extended outage, the AC
//    stays in the last known state instead of flapping.
// =============================================================

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <time.h>

#include <IRremoteESP8266.h>
#include <IRsend.h>
#include <ir_Fujitsu.h>

#include "config.h"

// ---- IR driver ----
IRFujitsuAC ac(IR_SEND_PIN);

// ---- Local state ----
enum AcState { AC_UNKNOWN, AC_ON, AC_OFF };
AcState localAcState = AC_UNKNOWN;

unsigned long lastStatePollAt = 0;
unsigned long lastHeartbeatAt = 0;
unsigned long currentBackoffMs = STATE_POLL_INTERVAL_MS;
unsigned long lastWifiRetryAt = 0;

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
    Serial.printf("[wifi] connected, ip=%s\n", WiFi.localIP().toString().c_str());
  } else {
    Serial.println("[wifi] connect failed");
  }
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
    Serial.println("[ntp] sync failed (will retry implicitly)");
  }
}

// =============================================================
// HTTP helpers
// =============================================================
int httpGet(const String& path, String& outBody) {
  HTTPClient http;
  http.setTimeout(HTTP_TIMEOUT_MS);
  String url = String(API_BASE_URL) + path;
  if (!http.begin(url)) return -1;
  http.addHeader("X-Device-Key", DEVICE_API_KEY);
  int code = http.GET();
  if (code > 0) outBody = http.getString();
  http.end();
  return code;
}

int httpPost(const String& path, const String& body) {
  HTTPClient http;
  http.setTimeout(HTTP_TIMEOUT_MS);
  String url = String(API_BASE_URL) + path;
  if (!http.begin(url)) return -1;
  http.addHeader("X-Device-Key", DEVICE_API_KEY);
  http.addHeader("Content-Type", "application/json");
  int code = http.POST(body);
  http.end();
  return code;
}

// =============================================================
// Logging to backend (best-effort, ignores errors)
// =============================================================
void postLog(const char* action, const char* detail) {
  if (WiFi.status() != WL_CONNECTED) return;
  StaticJsonDocument<192> doc;
  doc["action"] = action;
  if (detail) doc["detail"] = detail;
  String body;
  serializeJson(doc, body);
  int code = httpPost("/api/devices/me/log", body);
  Serial.printf("[log] %s -> %d\n", action, code);
}

void postHeartbeat() {
  if (WiFi.status() != WL_CONNECTED) return;
  StaticJsonDocument<128> doc;
  doc["ip"] = WiFi.localIP().toString();
  doc["rssi"] = WiFi.RSSI();
  String body;
  serializeJson(doc, body);
  int code = httpPost("/api/devices/me/heartbeat", body);
  Serial.printf("[heartbeat] -> %d\n", code);
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
    ac.send();  // resend once for reliability against IR loss
    Serial.println("[ir] AC -> ON");
    postLog("AC_ON", nullptr);
  } else if (desired == AC_OFF) {
    ac.off();
    ac.send();
    delay(150);
    ac.send();
    Serial.println("[ir] AC -> OFF");
    postLog("AC_OFF", nullptr);
  }
  localAcState = desired;
}

// =============================================================
// State polling
// =============================================================
bool pollAndApplyState() {
  String body;
  int code = httpGet("/api/devices/me/state", body);
  if (code != 200) {
    Serial.printf("[state] http=%d body=%s\n", code, body.c_str());
    return false;
  }

  StaticJsonDocument<1024> doc;
  DeserializationError err = deserializeJson(doc, body);
  if (err) {
    Serial.printf("[state] json error: %s\n", err.c_str());
    return false;
  }

  const char* desired = doc["desired"] | "OFF";
  const char* reason  = doc["reason"]  | "";
  bool holidayToday   = doc["holidayToday"] | false;
  const char* room    = doc["roomIdentification"] | "?";

  Serial.printf("[state] desired=%s reason=%s holiday=%d room=%s\n",
                desired, reason, holidayToday ? 1 : 0, room);

  AcState target = (strcmp(desired, "ON") == 0) ? AC_ON : AC_OFF;
  driveAc(target);
  return true;
}

// =============================================================
// Setup / Loop
// =============================================================
void setup() {
  Serial.begin(115200);
  delay(200);
  Serial.println("\n[boot] EcoWinds ESP32 starting");

  ac.begin();
  ensureWifi();
  if (WiFi.status() == WL_CONNECTED) {
    syncTime();
    postLog("BOOT", WiFi.localIP().toString().c_str());
  }
}

void loop() {
  ensureWifi();

  unsigned long now = millis();

  if (WiFi.status() == WL_CONNECTED) {
    if (now - lastStatePollAt >= currentBackoffMs) {
      lastStatePollAt = now;
      bool ok = pollAndApplyState();
      if (ok) {
        currentBackoffMs = STATE_POLL_INTERVAL_MS;
      } else {
        currentBackoffMs = min(currentBackoffMs * 2, (unsigned long) STATE_POLL_BACKOFF_MAX_MS);
        Serial.printf("[state] backoff -> %lums\n", currentBackoffMs);
      }
    }

    if (now - lastHeartbeatAt >= HEARTBEAT_INTERVAL_MS) {
      lastHeartbeatAt = now;
      postHeartbeat();
    }
  }

  delay(500);
}
