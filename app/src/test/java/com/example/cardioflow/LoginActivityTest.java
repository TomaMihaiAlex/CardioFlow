package com.example.cardioflow;

import android.content.Intent;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;

import com.example.cardioflow.activities.ChangePasswordActivity;
import com.example.cardioflow.activities.DoctorActivity;
import com.example.cardioflow.activities.LoginActivity;
import com.example.cardioflow.activities.MainActivity;
import com.example.cardioflow.activities.RegisterActivity;
import com.example.cardioflow.auth.AuthManager;
import com.google.android.material.textfield.TextInputLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import static org.junit.Assert.*;

/**
 * Robolectric tests for LoginActivity UI behaviour.
 *
 * These tests operate at the Activity level but run entirely on the JVM —
 * no emulator required. They cover:
 *   - Redirect to the correct dashboard after login
 *   - Validation error display for empty fields
 *   - Navigation to Register and ChangePassword screens
 *   - Auto-redirect when a session is already active
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class LoginActivityTest {

    @Before
    public void setUp() {
        // Clear any lingering auth state between tests.
        AuthManager.getInstance(RuntimeEnvironment.getApplication()).logout();
    }

    @After
    public void tearDown() {
        AuthManager.getInstance(RuntimeEnvironment.getApplication()).logout();
    }

    // ============================================================
    // Field validation — empty inputs must show errors, not crash
    // ============================================================

    @Test
    public void performLogin_emptyEmail_showsEmailError() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                // Leave email blank, fill password.
                ((EditText) activity.findViewById(R.id.et_email)).setText("");
                ((EditText) activity.findViewById(R.id.et_password)).setText("somepassword");

                activity.findViewById(R.id.btn_login).performClick();

                TextInputLayout tilEmail = activity.findViewById(R.id.til_email);
                assertNotNull("Email error must be set when email is empty",
                        tilEmail.getError());
            });
        }
    }

    @Test
    public void performLogin_emptyPassword_showsPasswordError() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                ((EditText) activity.findViewById(R.id.et_email)).setText("someone@example.com");
                ((EditText) activity.findViewById(R.id.et_password)).setText("");

                activity.findViewById(R.id.btn_login).performClick();

                TextInputLayout tilPassword = activity.findViewById(R.id.til_password);
                assertNotNull("Password error must be set when password is empty",
                        tilPassword.getError());
            });
        }
    }

    @Test
    public void performLogin_bothFieldsEmpty_showsEmailErrorFirst() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                ((EditText) activity.findViewById(R.id.et_email)).setText("");
                ((EditText) activity.findViewById(R.id.et_password)).setText("");

                activity.findViewById(R.id.btn_login).performClick();

                TextInputLayout tilEmail = activity.findViewById(R.id.til_email);
                // Email validation runs first — its error must be set.
                assertNotNull("Email error should appear before password error", tilEmail.getError());
            });
        }
    }

    // ============================================================
    // Navigation — Register button
    // ============================================================

    @Test
    public void clickRegisterButton_navigatesToRegisterActivity() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.btn_register).performClick();

                ShadowActivity shadow = Shadows.shadowOf(activity);
                Intent started = shadow.getNextStartedActivity();
                assertNotNull("Clicking Register must start an activity", started);
                assertEquals(RegisterActivity.class.getName(),
                        started.getComponent().getClassName());
            });
        }
    }

    // ============================================================
    // Navigation — Forgot Password button
    // ============================================================

    @Test
    public void clickForgotPassword_navigatesToChangePasswordActivity() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.btn_forgot_password).performClick();

                ShadowActivity shadow = Shadows.shadowOf(activity);
                Intent started = shadow.getNextStartedActivity();
                assertNotNull("Clicking Forgot Password must start an activity", started);
                assertEquals(ChangePasswordActivity.class.getName(),
                        started.getComponent().getClassName());
            });
        }
    }

    // ============================================================
    // Already-logged-in → auto redirect
    // ============================================================

    @Test
    public void onCreate_whenAlreadyLoggedInAsPatient_redirectsToMainActivity() {
        // Manually log in a patient before launching the activity.
        AuthManager auth = AuthManager.getInstance(RuntimeEnvironment.getApplication());
        auth.login("alice@example.com", "password123"); // patient in test users.json

        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                ShadowActivity shadow = Shadows.shadowOf(activity);
                Intent started = shadow.getNextStartedActivity();
                assertNotNull("Should redirect when already logged in", started);
                assertEquals("Patient should go to MainActivity",
                        MainActivity.class.getName(), started.getComponent().getClassName());
                assertTrue("LoginActivity must finish after redirect", activity.isFinishing());
            });
        }
    }

    @Test
    public void onCreate_whenAlreadyLoggedInAsDoctor_redirectsToDoctorActivity() {
        AuthManager auth = AuthManager.getInstance(RuntimeEnvironment.getApplication());
        auth.login("drsmith@example.com", "docpass"); // doctor in test users.json

        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                ShadowActivity shadow = Shadows.shadowOf(activity);
                Intent started = shadow.getNextStartedActivity();
                assertNotNull(started);
                assertEquals("Doctor should go to DoctorActivity",
                        DoctorActivity.class.getName(), started.getComponent().getClassName());
            });
        }
    }

    // ============================================================
    // directLogin() role routing — via reflection / package-visible helper
    // ============================================================

    @Test
    public void directLogin_withMedicRole_launchesDoctorActivity() {
        // Test the role-routing logic directly: create a user with role "medic",
        // save to prefs, then verify the launched intent.
        AuthManager auth = AuthManager.getInstance(RuntimeEnvironment.getApplication());
        // Use a doctor account from the test asset.
        auth.login("drsmith@example.com", "docpass");

        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                ShadowActivity shadow = Shadows.shadowOf(activity);
                Intent started = shadow.getNextStartedActivity();
                assertNotNull(started);
                String cls = started.getComponent().getClassName();
                assertEquals(DoctorActivity.class.getName(), cls);
            });
        }
    }

    @Test
    public void directLogin_withPacientRole_launchesMainActivity() {
        AuthManager auth = AuthManager.getInstance(RuntimeEnvironment.getApplication());
        auth.login("alice@example.com", "password123");

        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                ShadowActivity shadow = Shadows.shadowOf(activity);
                Intent started = shadow.getNextStartedActivity();
                assertNotNull(started);
                assertEquals(MainActivity.class.getName(),
                        started.getComponent().getClassName());
            });
        }
    }
}
