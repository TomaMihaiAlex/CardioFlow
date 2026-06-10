package com.example.cardioflow.services;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.models.Measurement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BLEReceiverServiceTest {

    private DatabaseManager dbManager;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        dbManager = DatabaseManager.getInstance(context);
        dbManager.clearAllData();
    }

    @Test
    public void testJsonParsingFromEsp32() throws Exception {
        String mockJson = "{\"sensors\":{\"heart_rate_bpm\":95, \"spo2_percent\":99, \"temperature_c\":36.6, \"humidity_percent\":40.0}}";
        
        BLEReceiverService service = Robolectric.setupService(BLEReceiverService.class);
        
        java.lang.reflect.Method method = BLEReceiverService.class.getDeclaredMethod("parseAndProcess", String.class);
        method.setAccessible(true);
        method.invoke(service, mockJson);

        List<Measurement> readings = dbManager.getLastReadings(1);
        assertEquals(1, readings.size());
        assertEquals(95, readings.get(0).getHeartRate());
    }

    @Test
    public void testAutoReconnectionLogicInitialization() {
        BLEReceiverService service = Robolectric.setupService(BLEReceiverService.class);
        assertNotNull(service);
        // Additional tests for reconnection logic would involve triggering onConnectionStateChange via reflection
    }
}
