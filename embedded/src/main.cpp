#include <Arduino.h>
#include "config.h"
#include "data/measurement.h"
#include "data/thresholds.h"

void setup() {
    Serial.begin(115200);
    Thresholds t = defaultThresholds();
    Serial.printf("Default HR: %d-%d\n", t.hrMin, t.hrMax);
}

void loop() {
}
