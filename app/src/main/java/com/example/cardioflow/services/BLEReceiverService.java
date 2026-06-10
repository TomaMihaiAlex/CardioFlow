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
import android.bluetooth.BluetoothGattService;
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
    private String lastStatus = "Deconectat";

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
            broadcastStatus("Niciun dispozitiv selectat");
        }
        
        broadcastStatus(lastStatus);

        return START_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(String address) {
        if (bluetoothAdapter == null || address == null || isConnecting) return;

        isConnecting = true;
        broadcastStatus("Se conectează la " + address + "...");
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
                broadcastStatus("Conectat");
                
                // Request larger MTU for long JSON strings
                gatt.requestMtu(512);
                
                // Wait a bit before discovering services to let MTU settle
                new Handler(Looper.getMainLooper()).postDelayed(gatt::discoverServices, 1000);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnecting = false;
                Log.i(TAG, "Disconnected from GATT server.");
                updateNotification("Deconectat. Reîncercare...");
                broadcastStatus("Deconectat. Reîncercare...");
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
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                BluetoothGattCharacteristic txChar = null;

                if (service != null) {
                    txChar = service.getCharacteristic(TX_CHAR_UUID);
                }

                if (service == null || txChar == null) {
                    service = gatt.getService(UUID.fromString(AppConstants.NORDIC_UART_SERVICE));
                    if (service != null) {
                        txChar = service.getCharacteristic(UUID.fromString(AppConstants.NORDIC_TX_CHAR));
                    }
                }

                boolean atLeastOneEnabled = false;
                if (service == null || txChar == null) {
                    service = gatt.getService(UUID.fromString(AppConstants.CUSTOM_SERVICE_UUID));
                    if (service != null) {
                        txChar = service.getCharacteristic(UUID.fromString(AppConstants.CUSTOM_TX_CHAR_UUID));
                        
                        for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
                            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                boolean success = gatt.setCharacteristicNotification(c, true);
                                if (success) {
                                    BluetoothGattDescriptor d = c.getDescriptor(CCCD_UUID);
                                    if (d == null && !c.getDescriptors().isEmpty()) d = c.getDescriptors().get(0);
                                    
                                    if (d != null) {
                                        d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        gatt.writeDescriptor(d);
                                    }
                                    atLeastOneEnabled = true;
                                }
                                if (txChar == null) txChar = c;
                            }
                        }
                    }
                }

                if (service != null && txChar != null) {
                    if (!atLeastOneEnabled) {
                        gatt.setCharacteristicNotification(txChar, true);
                        BluetoothGattDescriptor descriptor = txChar.getDescriptor(CCCD_UUID);
                        if (descriptor == null && !txChar.getDescriptors().isEmpty()) {
                            descriptor = txChar.getDescriptors().get(0);
                        }

                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            if (gatt.writeDescriptor(descriptor)) {
                                broadcastStatus("Conectat. Aștept date...");
                            } else {
                                broadcastStatus("Eroare activare date");
                            }
                        } else {
                            broadcastStatus("Conectat (Fără CCCD). Aștept date...");
                        }
                    } else {
                        broadcastStatus("Conectat. Aștept date...");
                    }
                } else {
                    broadcastStatus("Eroare: Serviciu UART necompatibil");
                    StringBuilder debugMsg = new StringBuilder("UUID-uri complete găsite:\n");
                    for (BluetoothGattService s : gatt.getServices()) {
                        debugMsg.append("S: ").append(s.getUuid().toString()).append("\n");
                        for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                            debugMsg.append("  C: ").append(c.getUuid().toString()).append("\n");
                        }
                    }
                    broadcastDebugInfo(debugMsg.toString());
                }
            } else {
                broadcastStatus("Eroare descoperire servicii: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            if (value == null) return;
            String part = new String(value);
            
            jsonBuffer.append(part);
            
            String fullString = jsonBuffer.toString();
            
            // AGGRESSIVE BUFFER FIX:
            // If we have multiple '{' without a '}', it means the previous one was incomplete
            // and the hardware restarted the transmission. We keep only the last one.
            int lastStart = fullString.lastIndexOf("{");
            int firstEnd = fullString.indexOf("}");
            
            if (lastStart > 0 && (firstEnd == -1 || firstEnd < lastStart)) {
                // Discard everything before the latest '{'
                String fixedString = fullString.substring(lastStart);
                jsonBuffer.setLength(0);
                jsonBuffer.append(fixedString);
                fullString = fixedString;
            }

            broadcastDebugInfo("BUFFER: " + fullString);
            
            int startIdx = fullString.indexOf("{");
            int endIdx = fullString.indexOf("}");
            
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                String completeJson = fullString.substring(startIdx, endIdx + 1);
                parseAndProcess(completeJson);
                
                // Clear the buffer after a successful (or attempted) full packet
                jsonBuffer.setLength(0);
                String remaining = fullString.substring(endIdx + 1);
                jsonBuffer.append(remaining);
            }
            
            if (jsonBuffer.length() > 0) {
                broadcastStatus("Recepție... (" + jsonBuffer.length() + " bytes)");
            }
        }
    };

    private void broadcastStatus(String status) {
        lastStatus = status;
        Intent intent = new Intent("com.example.cardioflow.BLE_STATUS_CHANGED");
        intent.putExtra("status", status);
        sendBroadcast(intent);
    }

    private void broadcastDebugInfo(String debugInfo) {
        Intent intent = new Intent("com.example.cardioflow.BLE_STATUS_CHANGED");
        intent.putExtra("status", lastStatus);
        intent.putExtra("debug_uuids", debugInfo);
        sendBroadcast(intent);
    }

    private void parseAndProcess(String json) {
        Log.d(TAG, "Parsing JSON: " + json);
        try {
            SensorReading reading = gson.fromJson(json, SensorReading.class);
            if (reading != null) {
                // Using the flat fields from the updated model
                int hr = reading.heartRate;
                int spo2 = reading.spo2;
                double temp = reading.temperature;
                double hum = reading.humidity;

                broadcastStatus("Date primite: HR=" + hr + " SpO2=" + spo2);
                dbManager.insertSensorData(hr, spo2, temp, hum);

                Intent intent = new Intent("com.example.cardioflow.BLE_DATA_RECEIVED");
                intent.putExtra("heartRate", hr);
                intent.putExtra("spo2", spo2);
                intent.putExtra("temp", temp);
                intent.putExtra("hum", hum);
                
                if (reading.ecgSamples != null) {
                    int[] samples = new int[reading.ecgSamples.size()];
                    for (int i = 0; i < reading.ecgSamples.size(); i++) samples[i] = reading.ecgSamples.get(i);
                    intent.putExtra("ecgSamples", samples);
                }

                // The hardware seems to use 'valid' flag instead of explicit leadsOff
                intent.putExtra("leadsOff", !reading.valid && hr == -1);
                sendBroadcast(intent);
            } else {
                broadcastStatus("Eroare: Obiect JSON nul");
            }
        } catch (Exception e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            broadcastStatus("Eroare format date (JSON)");
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
        broadcastStatus("Deconectat de utilizator");
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
