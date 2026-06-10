package com.example.cardioflow.fragments;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.example.cardioflow.R;
import com.example.cardioflow.auth.AuthManager;
import com.example.cardioflow.utils.AppConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU) // Use SDK 33 for better stability with Material3
public class HomeFragmentTest {

    private SharedPreferences prefs;

    @Before
    public void setUp() {
        AuthManager.resetInstance();
        Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_CardioFlow);
        
        // Log in as Alice (patient) to ensure thresholds are loaded
        AuthManager.getInstance(context).login("alice@example.com", "password123");
        
        prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    @After
    public void tearDown() {
        AuthManager.resetInstance();
    }

    @Test
    public void testBleVsSimulationToggle() {
        // Test with simulation ACTIVE
        prefs.edit().putBoolean(AppConstants.KEY_SIMULATION_MODE, true).commit();
        
        FragmentScenario<HomeFragment> scenario = FragmentScenario.launchInContainer(HomeFragment.class, null, R.style.Theme_CardioFlow, Lifecycle.State.RESUMED);
        scenario.onFragment(fragment -> {
            try {
                java.lang.reflect.Field field = HomeFragment.class.getDeclaredField("isSimulationMode");
                field.setAccessible(true);
                Object value = field.get(fragment);
                assertNotNull("isSimulationMode field should not be null", value);
                assertTrue("Simulation mode should be active", (Boolean) value);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void testAlarmThresholdPersistence() {
        FragmentScenario<HomeFragment> scenario = FragmentScenario.launchInContainer(HomeFragment.class, null, R.style.Theme_CardioFlow, Lifecycle.State.RESUMED);
        scenario.onFragment(fragment -> {
            try {
                java.lang.reflect.Method method = HomeFragment.class.getDeclaredMethod("checkThresholds", int.class, int.class, double.class, double.class);
                method.setAccessible(true);
                
                // Simulate abnormal heart rate (over default 100)
                method.invoke(fragment, 150, 98, 36.6, 45.0);
                
                java.lang.reflect.Field counterField = HomeFragment.class.getDeclaredField("alarmPersistCounter");
                counterField.setAccessible(true);
                Object count = counterField.get(fragment);
                assertNotNull("alarmPersistCounter should not be null", count);
                assertEquals("Persistence counter should be 1", 1, (int) count);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });
    }
}
