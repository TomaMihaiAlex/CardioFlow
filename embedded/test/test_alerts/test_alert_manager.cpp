#include <unity.h>
#include "../../src/alerts/alert_manager.h"
#include "../../src/data/thresholds.h"
#include <string.h>

static Thresholds makeThresholds() {
    Thresholds t{};
    strncpy(t.patientId, "1", sizeof(t.patientId));
    t.hrMin = 50; t.hrMax = 100;
    t.spo2Min = 90;
    t.tempMin = 35.5f; t.tempMax = 37.5f;
    t.humMin = 30.0f; t.humMax = 70.0f;
    t.persistSeconds = 10;
    t.activityIntervalMinutes = 5;
    return t;
}

static Measurement makeMeasurement(int hr, int spo2, float temp) {
    Measurement m{};
    strncpy(m.patientId, "1", sizeof(m.patientId));
    m.heartRate = hr; m.spo2 = spo2;
    m.temperature = temp; m.humidity = 45.0f;
    m.valid = true;
    return m;
}

void setUp() {}
void tearDown() {}

void test_no_alert_when_normal() {
    Measurement m = makeMeasurement(75, 98, 36.6f);
    Thresholds t = makeThresholds();
    AlertResult r = checkThresholds(m, t);
    TEST_ASSERT_FALSE(r.triggered);
}

void test_high_heart_rate_triggers_alert() {
    Measurement m = makeMeasurement(120, 98, 36.6f);
    Thresholds t = makeThresholds();
    AlertResult r = checkThresholds(m, t);
    TEST_ASSERT_TRUE(r.triggered);
    TEST_ASSERT_EQUAL_STRING("high_heart_rate", r.type);
    TEST_ASSERT_EQUAL_STRING("high", r.severity);
    TEST_ASSERT_EQUAL_FLOAT(120.0f, r.value);
}

void test_low_heart_rate_triggers_alert() {
    Measurement m = makeMeasurement(40, 98, 36.6f);
    Thresholds t = makeThresholds();
    AlertResult r = checkThresholds(m, t);
    TEST_ASSERT_TRUE(r.triggered);
    TEST_ASSERT_EQUAL_STRING("low_heart_rate", r.type);
    TEST_ASSERT_EQUAL_STRING("high", r.severity);
}

void test_low_spo2_triggers_alert() {
    Measurement m = makeMeasurement(75, 85, 36.6f);
    Thresholds t = makeThresholds();
    AlertResult r = checkThresholds(m, t);
    TEST_ASSERT_TRUE(r.triggered);
    TEST_ASSERT_EQUAL_STRING("low_spo2", r.type);
    TEST_ASSERT_EQUAL_STRING("high", r.severity);
}

void test_high_temp_triggers_medium_alert() {
    Measurement m = makeMeasurement(75, 98, 38.5f);
    Thresholds t = makeThresholds();
    AlertResult r = checkThresholds(m, t);
    TEST_ASSERT_TRUE(r.triggered);
    TEST_ASSERT_EQUAL_STRING("high_temp", r.type);
    TEST_ASSERT_EQUAL_STRING("medium", r.severity);
}

void test_low_temp_triggers_medium_alert() {
    Measurement m = makeMeasurement(75, 98, 34.0f);
    Thresholds t = makeThresholds();
    AlertResult r = checkThresholds(m, t);
    TEST_ASSERT_TRUE(r.triggered);
    TEST_ASSERT_EQUAL_STRING("low_temp", r.type);
    TEST_ASSERT_EQUAL_STRING("medium", r.severity);
}

void test_invalid_sensor_does_not_trigger() {
    Measurement m = makeMeasurement(-1, -1, -1.0f);
    Thresholds t = makeThresholds();
    AlertResult r = checkThresholds(m, t);
    TEST_ASSERT_FALSE(r.triggered);
}

void test_hr_at_exact_boundary_does_not_trigger() {
    // hrMax = 100, heartRate = 100 → NOT > hrMax, no alert
    Measurement m = makeMeasurement(100, 98, 36.6f);
    Thresholds t = makeThresholds();
    AlertResult r = checkThresholds(m, t);
    TEST_ASSERT_FALSE(r.triggered);
}

void test_build_alert_json_format() {
    AlertResult alert{};
    alert.triggered = true;
    strncpy(alert.type, "high_heart_rate", sizeof(alert.type) - 1);
    alert.value = 120.0f;
    strncpy(alert.severity, "high", sizeof(alert.severity) - 1);

    char buf[256];
    buildAlertJson(alert, "1", buf, sizeof(buf));
    TEST_ASSERT_EQUAL_STRING(
        "{\"patientId\":\"1\",\"type\":\"high_heart_rate\",\"value\":120.0,\"severity\":\"high\"}",
        buf
    );
}

int main() {
    UNITY_BEGIN();
    RUN_TEST(test_no_alert_when_normal);
    RUN_TEST(test_high_heart_rate_triggers_alert);
    RUN_TEST(test_low_heart_rate_triggers_alert);
    RUN_TEST(test_low_spo2_triggers_alert);
    RUN_TEST(test_high_temp_triggers_medium_alert);
    RUN_TEST(test_low_temp_triggers_medium_alert);
    RUN_TEST(test_invalid_sensor_does_not_trigger);
    RUN_TEST(test_hr_at_exact_boundary_does_not_trigger);
    RUN_TEST(test_build_alert_json_format);
    return UNITY_END();
}
