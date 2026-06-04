#pragma once

// DHT11
#define DHT_PIN         4

// AD8232 ECG
#define ECG_OUTPUT_PIN  34   // ADC input (doar citire)
#define ECG_LO_PLUS     32
#define ECG_LO_MINUS    33

// MAX30102 I2C
#define I2C_SDA_PIN  21
#define I2C_SCL_PIN  22

// DHT sensor type
#define DHT_SENSOR_TYPE  DHT11

// Timing
#define MEASUREMENT_INTERVAL_MS  10000   // 10 secunde
#define ECG_SAMPLE_RATE_HZ       250
#define ECG_PACKET_SAMPLES       10      // 10 mostre per pachet BLE = 20 bytes

// BLE
#define BLE_DEVICE_NAME  "CardioFlow"

// Buffer ECG circular (stochează 1 secundă de date)
#define ECG_BUFFER_SIZE  250
