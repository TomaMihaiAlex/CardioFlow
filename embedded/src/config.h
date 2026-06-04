#pragma once

// DHT11
#define DHT_PIN         4

// AD8232 ECG
#define ECG_OUTPUT_PIN  34   // ADC input (doar citire)
#define ECG_LO_PLUS     32
#define ECG_LO_MINUS    33

// MAX30102 folosește I2C implicit: SDA=21, SCL=22

// Timing
#define MEASUREMENT_INTERVAL_MS  10000   // 10 secunde
#define ECG_SAMPLE_RATE_HZ       250
#define ECG_PACKET_SAMPLES       10      // 10 mostre per pachet BLE = 20 bytes

// BLE
#define BLE_DEVICE_NAME  "CardioFlow"

// Buffer ECG circular (stochează 1 secundă de date)
#define ECG_BUFFER_SIZE  250
