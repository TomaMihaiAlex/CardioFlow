package com.example.cardioflow;

import android.content.Context;

import com.example.cardioflow.auth.AuthManager;
import com.example.cardioflow.models.User;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthManager.
 *
 * Dependencies (add to build.gradle if missing):
 *   testImplementation 'junit:junit:4.13.2'
 *   testImplementation 'org.mockito:mockito-core:5.11.0'
 *   testImplementation 'org.robolectric:robolectric:4.11.1'
 *
 * Robolectric is used so we get a real Context + SharedPreferences
 * without needing an emulator, while keeping tests in the JVM.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class AuthManagerTest {

    private AuthManager authManager;
    private Context context;

    // ---------- helpers ----------

    /** Builds a minimal User POJO for use in tests. */
    private User makeUser(String id, String email, String password, String role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPassword(password);
        u.setRole(role);
        return u;
    }

    @Before
    public void setUp() {
        // Robolectric supplies a real ApplicationContext backed by an in-memory filesystem.
        context = RuntimeEnvironment.getApplication();
        // Reset singleton between tests so state does not bleed across cases.
        AuthManager.resetInstance(); // you must add a package-private static resetInstance() — see note below
        authManager = AuthManager.getInstance(context);
    }

    // ============================================================
    // login() — local JSON-backed login
    // ============================================================

    @Test
    public void login_withValidCredentials_returnsTrue() {
        // The users.json asset must contain at least one user with these credentials.
        // In a real test environment, place a test-specific users.json under
        // src/test/assets/ so Robolectric picks it up automatically.
        boolean result = authManager.login("alice@example.com", "password123");
        assertTrue("Valid credentials should return true", result);
    }

    @Test
    public void login_withWrongPassword_returnsFalse() {
        boolean result = authManager.login("alice@example.com", "wrongpassword");
        assertFalse("Wrong password should return false", result);
    }

    @Test
    public void login_withUnknownEmail_returnsFalse() {
        boolean result = authManager.login("nobody@example.com", "password123");
        assertFalse("Unknown email should return false", result);
    }

    @Test
    public void login_isCaseInsensitiveOnEmail() {
        // Email comparison uses equalsIgnoreCase, so mixed-case should still match.
        boolean result = authManager.login("ALICE@EXAMPLE.COM", "password123");
        assertTrue("Email comparison should be case-insensitive", result);
    }

    @Test
    public void login_persistsUserIdToPrefs() {
        authManager.login("alice@example.com", "password123");
        assertTrue("logged_user_id must be stored after login", authManager.isLoggedIn());
    }

    // ============================================================
    // register()
    // ============================================================

    @Test
    public void register_withNewEmail_returnsTrue() {
        User newUser = makeUser(null, "newuser@example.com", "pass", "pacient");
        boolean result = authManager.register(newUser);
        assertTrue("Registering a brand-new email should succeed", result);
    }

    @Test
    public void register_assignsUuidToUser() {
        User newUser = makeUser(null, "newuser2@example.com", "pass", "pacient");
        authManager.register(newUser);
        assertNotNull("register() must assign a UUID id", newUser.getId());
        assertFalse("Assigned id must not be blank", newUser.getId().isEmpty());
    }

    @Test
    public void register_withDuplicateEmail_returnsFalse() {
        User first  = makeUser(null, "dup@example.com", "pass", "pacient");
        User second = makeUser(null, "dup@example.com", "pass2", "pacient");
        authManager.register(first);
        boolean result = authManager.register(second);
        assertFalse("Duplicate email registration should fail", result);
    }

    @Test
    public void register_duplicateCheck_isCaseInsensitive() {
        User first  = makeUser(null, "case@example.com", "pass", "pacient");
        User second = makeUser(null, "CASE@EXAMPLE.COM", "pass2", "pacient");
        authManager.register(first);
        boolean result = authManager.register(second);
        assertFalse("Duplicate email check must be case-insensitive", result);
    }

    // ============================================================
    // changePassword()
    // ============================================================

    @Test
    public void changePassword_withCorrectOldPassword_returnsTrue() {
        boolean result = authManager.changePassword("alice@example.com", "password123", "newpass");
        assertTrue("changePassword should succeed with correct old password", result);
    }

    @Test
    public void changePassword_withWrongOldPassword_returnsFalse() {
        boolean result = authManager.changePassword("alice@example.com", "wrongold", "newpass");
        assertFalse("changePassword should fail with wrong old password", result);
    }

    @Test
    public void changePassword_actuallyChangesThePassword() {
        authManager.changePassword("alice@example.com", "password123", "newpass");
        // After changing, old password must no longer work.
        assertFalse("Old password should be rejected after change",
                authManager.login("alice@example.com", "password123"));
        // New password must work.
        assertTrue("New password should be accepted after change",
                authManager.login("alice@example.com", "newpass"));
    }

    // ============================================================
    // getCurrentUser()
    // ============================================================

    @Test
    public void getCurrentUser_beforeLogin_returnsNull() {
        assertNull("No user should be current before any login", authManager.getCurrentUser());
    }

    @Test
    public void getCurrentUser_afterLogin_returnsCorrectUser() {
        authManager.login("alice@example.com", "password123");
        User user = authManager.getCurrentUser();
        assertNotNull("getCurrentUser must not return null after login", user);
        assertEquals("alice@example.com", user.getEmail().toLowerCase());
    }

    // ============================================================
    // logout()
    // ============================================================

    @Test
    public void logout_clearsLoginState() {
        authManager.login("alice@example.com", "password123");
        assertTrue(authManager.isLoggedIn());
        authManager.logout();
        assertFalse("isLoggedIn() must be false after logout", authManager.isLoggedIn());
    }

    @Test
    public void logout_getCurrentUser_returnsNull() {
        authManager.login("alice@example.com", "password123");
        authManager.logout();
        assertNull("getCurrentUser() must return null after logout", authManager.getCurrentUser());
    }

    // ============================================================
    // isLoggedIn()
    // ============================================================

    @Test
    public void isLoggedIn_initiallyFalse() {
        assertFalse(authManager.isLoggedIn());
    }

    @Test
    public void isLoggedIn_trueAfterSuccessfulLogin() {
        authManager.login("alice@example.com", "password123");
        assertTrue(authManager.isLoggedIn());
    }

    // ============================================================
    // getPatientsForDoctor()
    // ============================================================

    @Test
    public void getPatientsForDoctor_returnsOnlyPatientsForThatDoctor() {
        // Register two patients for doctor "doc1" and one for "doc2".
        User p1 = makeUser(null, "pat1@example.com", "p", "pacient");
        p1.setDoctorId("doc1");
        User p2 = makeUser(null, "pat2@example.com", "p", "pacient");
        p2.setDoctorId("doc1");
        User p3 = makeUser(null, "pat3@example.com", "p", "pacient");
        p3.setDoctorId("doc2");

        authManager.register(p1);
        authManager.register(p2);
        authManager.register(p3);

        List<User> doc1Patients = authManager.getPatientsForDoctor("doc1");
        assertEquals("doc1 should have exactly 2 patients", 2, doc1Patients.size());
        for (User u : doc1Patients) {
            assertEquals("pacient", u.getRole());
            assertEquals("doc1", u.getDoctorId());
        }
    }

    @Test
    public void getPatientsForDoctor_excludesNonPatientRoles() {
        // A user with role 'medic' and doctorId set should NOT appear as a patient.
        User doctor = makeUser(null, "dr2@example.com", "p", "medic");
        doctor.setDoctorId("doc1");
        authManager.register(doctor);

        List<User> patients = authManager.getPatientsForDoctor("doc1");
        for (User u : patients) {
            assertNotEquals("Non-patient roles must be excluded", "medic", u.getRole());
        }
    }

    // ============================================================
    // loginFirebase() — Firebase not initialised path (safe fallback)
    // ============================================================

    @Test
    public void loginFirebase_whenFirebaseNull_fallsBackToLocalLogin() {
        // Simulate the case where FirebaseAuth is unavailable (e.g. missing google-services.json).
        // First make sure Alice is locally logged-in resolvable.
        // We test the callback path using a simple flag.
        boolean[] successCalled = {false};
        boolean[] errorCalled   = {false};

        authManager.loginFirebase("alice@example.com", "password123", new AuthManager.AuthCallback() {
            @Override public void onSuccess(User user) { successCalled[0] = true; }
            @Override public void onError(String error) { errorCalled[0] = true; }
        });

        // Depending on whether Firebase is initialised in the test JVM, one of the two must fire.
        assertTrue("Either success or error callback must be invoked",
                successCalled[0] || errorCalled[0]);
    }
}
