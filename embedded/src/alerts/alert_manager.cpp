#include "alert_manager.h"
#include <stdio.h>
#include <string.h>

AlertResult checkThresholds(const Measurement& m, const Thresholds& t) {
    AlertResult result{};
    result.triggered = false;

    if (m.heartRate != -1 && m.heartRate > t.hrMax) {
        result.triggered = true;
        strncpy(result.type, "high_heart_rate", sizeof(result.type) - 1);
        result.value = (float)m.heartRate;
        strncpy(result.severity, "high", sizeof(result.severity) - 1);
    } else if (m.heartRate != -1 && m.heartRate < t.hrMin) {
        result.triggered = true;
        strncpy(result.type, "low_heart_rate", sizeof(result.type) - 1);
        result.value = (float)m.heartRate;
        strncpy(result.severity, "high", sizeof(result.severity) - 1);
    } else if (m.spo2 != -1 && m.spo2 < t.spo2Min) {
        result.triggered = true;
        strncpy(result.type, "low_spo2", sizeof(result.type) - 1);
        result.value = (float)m.spo2;
        strncpy(result.severity, "high", sizeof(result.severity) - 1);
    } else if (m.temperature != -1.0f && m.temperature > t.tempMax) {
        result.triggered = true;
        strncpy(result.type, "high_temp", sizeof(result.type) - 1);
        result.value = m.temperature;
        strncpy(result.severity, "medium", sizeof(result.severity) - 1);
    } else if (m.temperature != -1.0f && m.temperature < t.tempMin) {
        result.triggered = true;
        strncpy(result.type, "low_temp", sizeof(result.type) - 1);
        result.value = m.temperature;
        strncpy(result.severity, "medium", sizeof(result.severity) - 1);
    }

    return result;
}

void buildAlertJson(const AlertResult& alert, const char* patientId, char* outBuf, size_t bufSize) {
    snprintf(outBuf, bufSize,
        "{\"patientId\":\"%s\",\"type\":\"%s\",\"value\":%.1f,\"severity\":\"%s\"}",
        patientId, alert.type, alert.value, alert.severity
    );
}
