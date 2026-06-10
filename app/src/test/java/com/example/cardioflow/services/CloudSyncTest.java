package com.example.cardioflow.services;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import com.example.cardioflow.database.DatabaseHelper;
import com.example.cardioflow.models.Measurement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import androidx.work.testing.WorkManagerTestInitHelper;

@RunWith(RobolectricTestRunner.class)
public class CloudSyncTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        WorkManagerTestInitHelper.initializeTestWorkManager(context);
        // Clear offline requests
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("offline_requests", null, null);
        db.close();
    }

    @Test
    public void testOfflineStorageOnSyncFailure() {
        Measurement m = new Measurement("p1", "2023-10-27", 100, 95, 37.0, 50.0);
        
        // This will attempt to send to Firestore, fail in test env, and save to SQLite
        CloudSync.sendAggregatedData(context, m);

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("offline_requests", null, null, null, null, null, null);
        
        assertTrue("Offline requests should save to SQLite if Firestore fails", cursor.getCount() > 0);
        
        cursor.close();
        db.close();
    }
}
