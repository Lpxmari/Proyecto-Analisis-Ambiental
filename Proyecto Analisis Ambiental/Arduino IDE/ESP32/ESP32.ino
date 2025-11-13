#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <Wire.h>
#include <SPI.h>

// ------------------ CONFIG ------------------
const char* ssid = "dave";
const char* password = "pruebaproyecto";
const char* serverUrl = "http://10.166.70.200:8080/datos";

// Pines sensores
#define DHTPIN 4
#define DHTTYPE DHT22
#define MQ_PIN 34
#define UV_PIN 35
#define VBAT_PIN 36

// Periodos (ms)
static const unsigned long PERIOD_SEND = 10000UL;
static const unsigned long PERIOD_DHT  = 30000UL;
static const unsigned long PERIOD_MQ   = 120000UL;
static const unsigned long PERIOD_UV   = 120000UL;
static const unsigned long PERIOD_I2C  = 20000UL;

// ADC
static const float ADC_REF = 3.3f;
static const int   ADC_MAX = 4095;
static const float VBAT_SCALE = 3.0f;

// ------------------ Objetos ------------------
DHT dht(DHTPIN, DHTTYPE);

// Estructura de lecturas
struct Lectura {
  unsigned long ts;
  float temp;
  float hum;
  float mqVolt;
  float uvVolt;
  float vbat;

  // Métricas extendidas
  unsigned long i2cLatencyUs;
  unsigned long wifiBytesSent;
  unsigned long wifiBytesRecv;
  uint32_t commErrors;
  int wifiRSSI;
};
Lectura last = {0, NAN, NAN, NAN, NAN, NAN, 0, 0, 0, 0, 0};

// Timers
unsigned long lastSend = 0;
unsigned long lastDht  = 0;
unsigned long lastMQ   = 0;
unsigned long lastUV   = 0;
unsigned long lastI2C  = 0;

// Contadores WiFi y errores
uint32_t totalBytesSent = 0;
uint32_t totalBytesRecv = 0;
uint32_t commErrors = 0;

// ------------------ Funciones ------------------
float readMQ() {
  return (analogRead(MQ_PIN) * ADC_REF) / ADC_MAX;
}

float readUV() {
  return (analogRead(UV_PIN) * ADC_REF) / ADC_MAX;
}

float readVbat() {
  int raw = analogRead(VBAT_PIN);
  if (raw <= 1 || raw >= 4094) return NAN;
  return ((raw * ADC_REF) / ADC_MAX) * VBAT_SCALE;
}

unsigned long measureI2CLatency() {
  unsigned long t1 = micros();
  Wire.beginTransmission(0x76);  // Dirección típica de sensor (p. ej. BME280)
  Wire.write(0xD0);
  Wire.endTransmission();
  Wire.requestFrom(0x76, 1);
  while (Wire.available()) Wire.read();
  unsigned long t2 = micros();
  return t2 - t1;  // Latencia total en microsegundos
}

extern "C" int temprature_sens_read();

String getResetReason() {
  esp_reset_reason_t r = esp_reset_reason();
  switch (r) {
    case ESP_RST_POWERON: return "POWERON";
    case ESP_RST_SW:      return "SW_RESET";
    case ESP_RST_PANIC:   return "PANIC";
    case ESP_RST_INT_WDT: return "INT_WDT";
    case ESP_RST_WDT:     return "WDT";
    default: return "OTHER";
  }
}

// ------------------ Comunicación ------------------
bool enviarJSON(const String& payload) {
  if (WiFi.status() != WL_CONNECTED) return false;

  HTTPClient http;
  http.begin(serverUrl);
  http.addHeader("Content-Type", "application/json");

  int code = http.POST(payload);
  if (code > 0) {
    totalBytesSent += payload.length();
    totalBytesRecv += http.getSize();
  } else {
    commErrors++;
  }

  http.end();
  return code > 0;
}

// ------------------ SETUP ------------------
void setup() {
  Serial.begin(115200);
  delay(300);

  dht.begin();
  Wire.begin();
  SPI.begin();

  WiFi.begin(ssid, password);
  Serial.print("Conectando WiFi");
  unsigned long t0 = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - t0 < 15000) {
    Serial.print(".");
    delay(300);
  }
  Serial.println();
  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("IP: ");
    Serial.println(WiFi.localIP());
  }

  lastSend = millis();
  lastDht  = millis() - PERIOD_DHT;
  lastMQ   = millis() - PERIOD_MQ;
  lastUV   = millis() - PERIOD_UV;
  lastI2C  = millis() - PERIOD_I2C;
}

// ------------------ LOOP ------------------
void loop() {
  unsigned long now = millis();

  // Lectura DHT
  if (now - lastDht >= PERIOD_DHT) {
    lastDht = now;
    float t = dht.readTemperature();
    float h = dht.readHumidity();
    if (!isnan(t) && !isnan(h)) {
      last.temp = t;
      last.hum = h;
    } else {
      Serial.println("Error DHT.");
      commErrors++;
    }
  }

  // Lectura MQ
  if (now - lastMQ >= PERIOD_MQ) {
    lastMQ = now;
    float mq = readMQ();
    last.mqVolt = (mq >= 0.0f && mq <= 5.5f) ? mq : NAN;
  }

  // Lectura UV
  if (now - lastUV >= PERIOD_UV) {
    lastUV = now;
    float uv = readUV();
    last.uvVolt = (uv >= 0.0f && uv <= 5.5f) ? uv : NAN;
  }

  // Lectura batería
  last.vbat = readVbat();

  // Simulación I2C / Latencia
  if (now - lastI2C >= PERIOD_I2C) {
    lastI2C = now;
    last.i2cLatencyUs = measureI2CLatency();
  }

  // Métricas WiFi y errores
  last.wifiRSSI = WiFi.RSSI();
  last.wifiBytesSent = totalBytesSent;
  last.wifiBytesRecv = totalBytesRecv;
  last.commErrors = commErrors;

  last.ts = now;

  // Envío periódico
  if (now - lastSend >= PERIOD_SEND) {
    lastSend = now;

    DynamicJsonDocument doc(1024);
    doc["device"] = "ESP32_estacion";
    doc["ts"] = last.ts;

    JsonObject s = doc.createNestedObject("sensors");
    if (!isnan(last.temp)) s["temp"] = last.temp;
    if (!isnan(last.hum))  s["hum"] = last.hum;
    if (!isnan(last.mqVolt)) s["mqVolt"] = last.mqVolt;
    if (!isnan(last.uvVolt)) s["uvVolt"] = last.uvVolt;
    if (!isnan(last.vbat)) s["vbat"] = last.vbat;

    JsonObject m = doc.createNestedObject("metrics");
    m["freeHeap"] = ESP.getFreeHeap();
    m["cpuFreqMHz"] = ESP.getCpuFreqMHz();
    m["internalTemp"] = (float)temprature_sens_read();
    m["resetReason"] = getResetReason();
    m["i2cLatencyUs"] = last.i2cLatencyUs;
    m["wifiRSSI"] = last.wifiRSSI;
    m["wifiBytesSent"] = last.wifiBytesSent;
    m["wifiBytesRecv"] = last.wifiBytesRecv;
    m["commErrors"] = last.commErrors;

    String payload;
    serializeJson(doc, payload);

    Serial.print("Enviando: ");
    Serial.println(payload);

    if (!enviarJSON(payload)) {
      Serial.println("Error enviando JSON.");
    } else {
      Serial.println("JSON enviado correctamente.");
    }
  }

  delay(30);
}
