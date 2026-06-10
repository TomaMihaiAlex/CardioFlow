package com.example.cardioflow.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.cardioflow.R;
import com.example.cardioflow.activities.MainActivity;
import com.example.cardioflow.auth.AuthManager;
import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.models.SensorReading;
import com.example.cardioflow.models.User;
import com.example.cardioflow.utils.AppConstants;
import com.google.gson.Gson;

import java.util.UUID;

public class BLEReceiverService extends Service {
    private static final String TAG = "BLEReceiverService";
    private static final String CHANNEL_ID = "BLE_Service_Channel";
    private static final int NOTIFICATION_ID = 1001;

    // UUIDs for ESP32 UART Service (Standard Nordic UART or similar)
    private static final UUID UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID TX_CHARACTERISTIC_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private String deviceAddress;
    private StringBuilder jsonBuffer = new StringBuilder();
    private Gson gson = new Gson();
    private DatabaseManager dbManager;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isConnecting = false;

    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bm.getAdapter();
        dbManager = DatabaseManager.getInstance(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, getNotification("Căutare dispozitiv..."));

        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
        deviceAddress = prefs.getString("last_ble_address", null);

        if (intent != null && intent.hasExtra("device_address")) {
            deviceAddress = intent.getStringExtra("device_address");
            prefs.edit().putString("last_ble_address", deviceAddress).apply();
        }

        if (deviceAddress != null) {
            connectToDevice(deviceAddress);
        } else {
            updateNotification("Niciun dispozitiv salvat.");
        }

        return START_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(String address) {
        if (bluetoothAdapter == null || address == null || isConnecting) return;

        isConnecting = true;
        updateNotification("Conectare la " + address + "...");
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        bluetoothGatt = device.connectGatt(this, true, gattCallback); // Auto-connect true for stability
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnecting = false;
                Log.i(TAG, "Connected to GATT server.");
                updateNotification("Conectat la ESP32");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnecting = false;
                Log.i(TAG, "Disconnected from GATT server.");
                updateNotification("Deconectat. Reîncercare...");
                jsonBuffer.setLength(0);
                // The stack will try to reconnect automatically if autoConnect was true
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattCharacteristic txChar = gatt.getService(UART_SERVICE_UUID)
                        .getCharacteristic(TX_CHARACTERISTIC_UUID);
                
                gatt.setCharacteristicNotification(txChar, true);
                
                BluetoothGattDescriptor descriptor = txChar.getDescriptor(CCCD_UUID);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String part = new String(characteristic.getValue());
            jsonBuffer.append(part);
            
            String fullString = jsonBuffer.toString();
            if (fullString.contains("{") && fullString.contains("}")) {
                // Try to find a complete JSON object
                int start = fullString.indexOf("{");
                int end = fullString.lastIndexOf("}");
                if (end > start) {
                    String completeJson = fullString.substring(start, end + 1);
                    parseAndStoreData(completeJson);
                    jsonBuffer.delete(0, end + 1);
                }
            }
        }
    };

    private void parseAndStoreData(String json) {
        try {
            SensorReading reading = gson.fromJson(json, SensorReading.class);
            if (reading != null && reading.sensors != null) {
                User user = AuthManager.getInstance(this).getCurrentUser();
                String patientId = (user != null) ? user.getId() : "1";

                // Map to existing database structure
                dbManager.insertSensorData(
                        reading.sensors.heartRate,
                        reading.sensors.spo2,
                        reading.sensors.temperature,
                        reading.sensors.humidity
                );

                // Broadcast update for UI
                Intent intent = new Intent("com.example.cardioflow.BLE_DATA_RECEIVED");
                intent.putExtra("heartRate", reading.sensors.heartRate);
                intent.putExtra("spo2", reading.sensors.spo2);
                intent.putExtra("temp", reading.sensors.temperature);
                intent.putExtra("hum", reading.sensors.humidity);
                intent.putExtra("leadsOff", reading.hardwareStatus != null && reading.hardwareStatus.ecgLeadsOff);
                sendBroadcast(intent);
                
                Log.d(TAG, "Data saved and broadcasted: " + json);
            }
        } catch (Exception e) {
            Log.e(TAG, "JSON Parse Error: " + e.getMessage());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Serviciu Monitorizare BLE",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification getNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("CardioFlow BLE")
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, getNotification(text));
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
