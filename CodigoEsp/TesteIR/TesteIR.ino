// =============================================================
// Teste isolado do módulo IR — EcoWinds
//
// Sem WiFi, sem backend. Só fica alternando o ar Fujitsu
// ON -> OFF -> ON ... a cada 4 segundos.
//
// Como usar:
//   1. Abra este sketch na Arduino IDE.
//   2. Grave no ESP32.
//   3. Abra o Serial Monitor a 115200 baud.
//   4. Aponte a CÂMERA do celular pro LED IR: ele deve
//      piscar (luz branco-azulada) a cada envio.
//   5. Aponte o LED pro ar e veja se ele liga/desliga.
// =============================================================

#include <IRremoteESP8266.h>
#include <IRsend.h>
#include <ir_Fujitsu.h>

// Mesmo pino do projeto (config.h: IR_SEND_PIN 4)
#define IR_SEND_PIN 4

IRFujitsuAC ac(IR_SEND_PIN);

bool ligado = false;

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println("\n[teste-ir] iniciando — alterna ON/OFF a cada 4s no GPIO4");
  ac.begin();
}

void loop() {
  ligado = !ligado;

  if (ligado) {
    ac.on();
    ac.send();
    delay(150);
    ac.send();   // repete pra garantir recepção
    Serial.println("[teste-ir] enviado ON  -> olhe o LED pela camera");
  } else {
    ac.off();
    ac.send();
    delay(150);
    ac.send();
    Serial.println("[teste-ir] enviado OFF -> olhe o LED pela camera");
  }

  delay(4000);
}
