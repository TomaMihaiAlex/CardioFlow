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
import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.models.SensorReading;
import com.example.cardioflow.utils.AppConstants;
import com.google.gson.Gson;

import java.util.UUID;

public class BLEReceiverService extends Service {
    private static final String TAG = "BLEReceiverService";
    private static final String CHANNEL_ID = "BLE_Service_Channel";
    private static final int NOTIFICATION_ID = 1001;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private String deviceAddress;
    private final StringBuilder jsonBuffer = new StringBuilder();
    private final Gson gson = new Gson();
    private DatabaseManager dbManager;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private boolean isConnecting = false;
    private boolean isUserDisconnected = false;

    private static final UUID SERVICE_UUID = UUID.fromString(AppConstants.UART_SERVICE_UUID);
    private static final UUID TX_CHAR_UUID = UUID.fromString(AppConstants.TX_CHAR_UUID);
    private static final UUID CCCD_UUID = UUID.fromString(AppConstants.CCCD_UUID);

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
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, getNotification("Căutare dispozitiv..."));

        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
        
        if (intent != null && intent.hasExtra("device_address")) {
            deviceAddress = intent.getStringExtra("device_address");
            prefs.edit().putString(AppConstants.KEY_LAST_MAC, deviceAddress).apply();
        } else {
            deviceAddress = prefs.getString(AppConstants.KEY_LAST_MAC, null);
        }

        isUserDisconnected = false;
        if (deviceAddress != null) {
            connectToDevice(deviceAddress);
        } else {
            updateNotification("Niciun dispozitiv selectat.");
        }

        return START_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(String address) {
        if (bluetoothAdapter == null || address == null || isConnecting) return;

        isConnecting = true;
        updateNotification("Conectare la " + address + "...");
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnecting = false;
                Log.i(TAG, "Connected to GATT server.");
                updateNotification("Dispozitiv Conectat");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnecting = false;
                Log.i(TAG, "Disconnected from GATT server.");
                updateNotification("Deconectat. Reîncercare...");
                jsonBuffer.setLength(0);
                
                if (!isUserDisconnected) {
                    reconnectHandler.postDelayed(() -> connectToDevice(deviceAddress), 5000);
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                try {
                    BluetoothGattCharacteristic txChar = gatt.getService(SERVICE_UUID)
                            .getCharacteristic(TX_CHAR_UUID);
                    
                    gatt.setCharacteristicNotification(txChar, true);
                    
                    BluetoothGattDescriptor descriptor = txChar.getDescriptor(CCCD_UUID);
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error setting up notifications: " + e.getMessage());
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String part = new String(characteristic.getValue());
            jsonBuffer.append(part);
            
            String fullString = jsonBuffer.toString();
            int startIdx;
            while ((startIdx = fullString.indexOf("{")) != -1) {
                int endIdx = fullString.indexOf("}", startIdx);
                if (endIdx != -1) {
                    String completeJson = fullString.substring(startIdx, endIdx + 1);
                    parseAndProcess(completeJson);
                    fullString = fullString.substring(endIdx + 1);
                    jsonBuffer.setLength(0);
                    jsonBuffer.append(fullString);
                } else {
                    break;
                }
            }
        }
    };

    private void parseAndProcess(String json) {
        try {
            SensorReading reading = gson.fromJson(json, SensorReading.class);
            if (reading != null && reading.sensors != null) {
                dbManager.insertSensorData(
                        reading.sensors.heartRate,
                        reading.sensors.spo2,
                        reading.sensors.temperature,
                        reading.sensors.humidity
                );

                Intent intent = new Intent("com.example.cardioflow.BLE_DATA_RECEIVED");
                intent.putExtra("heartRate", reading.sensors.heartRate);
                intent.putExtra("spo2", reading.sensors.spo2);
                intent.putExtra("temp", reading.sensors.temperature);
                intent.putExtra("hum", reading.sensors.humidity);
                intent.putExtra("leadsOff", reading.hardwareStatus != null && reading.hardwareStatus.ecgLeadsOff);
                sendBroadcast(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
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
                .setContentTitle("CardioFlow Live")
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
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
        isUserDisconnected = true;
        reconnectHandler.removeCallbacksAndMessages(null);
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
