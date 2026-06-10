package com.example.cardioflow.database;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.example.cardioflow.data.DataManager;
import com.example.cardioflow.models.Measurement;
import com.example.cardioflow.models.Recommendation;
import com.example.cardioflow.models.Thresholds;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class DatabaseManagerTest {

    private DatabaseManager dbManager;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        dbManager = DatabaseManager.getInstance(context);
        dbManager.clearAllData();
    }

    @Test
    public void testAverageCalculationLogic() {
        // Simulating 3 readings to verify the average logic that will be sent to the Cloud
        dbManager.insertSensorData(80, 98, 36.0, 40.0);
        dbManager.insertSensorData(90, 96, 37.0, 50.0);
        dbManager.insertSensorData(70, 94, 38.0, 60.0);

        List<Measurement> readings = dbManager.getLastReadings(3);
        assertEquals(3, readings.size());
        
        double sumHr = 0;
        for (Measurement m : readings) {
            sumHr += m.getHeartRate();
        }
        
        double average = sumHr / readings.size();
        assertEquals(80.0, average, 0.1); // (80+90+70)/3 = 80
    }

    @Test
    public void testRecommendationPersistence() {
        Recommendation rec = new Recommendation("id123", "patient_01", "doctor_01", "Jogging", 30, "Zilnic", "low");
        dbManager.insertRecommendation(rec);
        
        List<Recommendation> list = dbManager.getRecommendationsForPatient("patient_01");
        assertEquals("Should have 1 recommendation", 1, list.size());
        assertEquals("Jogging", list.get(0).getType());
    }

    @Test
    public void testThresholdsPersistenceViaDataManager() {
        Context context = ApplicationProvider.getApplicationContext();
        DataManager dataManager = DataManager.getInstance(context);
        
        Thresholds t = new Thresholds();
        t.setPatientId("p_test");
        t.setHrMax(120);
        t.setHrMin(55);
        
        dataManager.updateThresholds(t);
        
        Thresholds retrieved = dataManager.getThresholdsForPatient("p_test");
        assertEquals(120, retrieved.getHrMax());
        assertEquals(55, retrieved.getHrMin());
    }
}
