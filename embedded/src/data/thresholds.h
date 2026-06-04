#pragma once
#include <string.h>

struct Thresholds {
    char patientId[16];
    int hrMin;
    int hrMax;
    int spo2Min;
    float tempMin;
    float tempMax;
    float humMin;
    float humMax;
    int persistSeconds;
    int activityIntervalMinutes;
};

// Valori default până la prima configurare de pe telefon
inline Thresholds defaultThresholds() {
    Thresholds t;
    strncpy(t.patientId, "1", sizeof(t.patientId));
    t.hrMin = 50;
    t.hrMax = 100;
    t.spo2Min = 90;
    t.tempMin = 35.5f;
    t.tempMax = 37.5f;
    t.humMin = 30.0f;
    t.humMax = 70.0f;
    t.persistSeconds = 10;
    t.activityIntervalMinutes = 5;
    return t;
}
