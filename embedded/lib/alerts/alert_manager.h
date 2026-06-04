#pragma once
#include <stdint.h>
#include <stdbool.h>
#include "../../src/data/measurement.h"
#include "../../src/data/thresholds.h"

struct AlertResult {
    bool triggered;
    char type[32];
    float value;
    char severity[8];  // "high" or "medium"
};

AlertResult checkThresholds(const Measurement& m, const Thresholds& t);
void buildAlertJson(const AlertResult& alert, const char* patientId, char* outBuf, size_t bufSize);
