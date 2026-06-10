package com.example.cardioflow.sync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cardioflow.database.DatabaseHelper;
import com.example.cardioflow.database.FirebaseManager;
import com.example.cardioflow.models.Alert;
import com.example.cardioflow.models.Measurement;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.Gson;

import java.util.concurrent.ExecutionException;

public class CloudSyncWorker extends Worker {
    private static final String TAG = "CloudSyncWorker";
    private final Gson gson = new Gson();

    public CloudSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Syncing offline requests...");
        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        Cursor cursor = db.query("offline_requests", null, null, null, null, null, "id ASC");
        
        boolean hasFailures = false;
        
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String type = cursor.getString(cursor.getColumnIndexOrThrow("url"));
                String payload = cursor.getString(cursor.getColumnIndexOrThrow("payload"));
                
                try {
                    boolean success = false;
                    if ("sensorData".equals(type)) {
                        Measurement m = gson.fromJson(payload, Measurement.class);
                        Tasks.await(FirebaseManager.getInstance().getSensorDataCollection().add(m));
                        success = true;
                    } else if ("alerts".equals(type)) {
                        Alert a = gson.fromJson(payload, Alert.class);
                        Tasks.await(FirebaseManager.getInstance().getAlertsCollection().add(a));
                        success = true;
                    }
                    
                    if (success) {
                        db.delete("offline_requests", "id = ?", new String[]{String.valueOf(id)});
                        Log.d(TAG, "Successfully synced record ID: " + id);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Failed to sync record " + id + ", will retry later", e);
                    hasFailures = true;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        
        return hasFailures ? Result.retry() : Result.success();
    }
}
