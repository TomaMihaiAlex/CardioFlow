package com.example.cardioflow.auth;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.cardioflow.database.FirebaseManager;
import com.example.cardioflow.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuthManager {
    private static AuthManager instance;
    private final Context context;
    private List<User> userList;
    private final SharedPreferences prefs;
    private final FirebaseAuth firebaseAuth;

    private AuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        this.firebaseAuth = safeGetFirebaseAuth();
        loadUsers();
    }


    private FirebaseAuth safeGetFirebaseAuth() {
        try {
            return FirebaseAuth.getInstance();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }
    public static void resetInstance() { instance = null; }
    private void loadUsers() {
        try {
            InputStream is = context.getAssets().open("users.json");
            InputStreamReader reader = new InputStreamReader(is);
            Type type = new TypeToken<ArrayList<User>>(){}.getType();
            userList = new Gson().fromJson(reader, type);
        } catch (Exception e) {
            userList = new ArrayList<>();
        }
    }

    public void loginFirebase(String email, String password, AuthCallback callback) {
        if (firebaseAuth == null) {
            // If Firebase is not initialized, try local login as a fallback
            if (login(email, password)) {
                User user = getCurrentUser();
                if (user != null) {
                    callback.onSuccess(user);
                } else {
                    callback.onError("Firebase not initialized and local user data not found");
                }
            } else {
                callback.onError("Firebase not initialized and local login failed");
            }
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fbUser = firebaseAuth.getCurrentUser();
                        if (fbUser != null) {
                            // Fetch user details from Firestore
                            FirebaseManager firebaseManager = FirebaseManager.getInstance();
                            if (firebaseManager.getUsersCollection() == null) {
                                callback.onError("Firebase Firestore not initialized");
                                return;
                            }
                            firebaseManager.getUsersCollection()
                                    .document(fbUser.getUid())
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        User user = documentSnapshot.toObject(User.class);
                                        if (user != null) {
                                            saveUserToPrefs(user);
                                            callback.onSuccess(user);
                                        } else {
                                            callback.onError("User data not found");
                                        }
                                    })
                                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
                        }
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Login failed");
                    }
                });
    }

    private void saveUserToPrefs(User user) {
        prefs.edit()
                .putString("logged_user_id", user.getId())
                .putString("logged_user_email", user.getEmail())
                .putString("logged_user_role", user.getRole())
                .apply();
    }

    public boolean login(String email, String password) {
        for (User user : userList) {
            if (user.getEmail().equalsIgnoreCase(email) && user.getPassword().equals(password)) {
                prefs.edit()
                    .putString("logged_user_id", user.getId())
                    .putString("logged_user_email", user.getEmail())
                    .putString("logged_user_role", user.getRole())
                    .apply();
                return true;
            }
        }
        return false;
    }

    public boolean register(User newUser) {
        for (User u : userList) {
            if (u.getEmail().equalsIgnoreCase(newUser.getEmail())) {
                return false;
            }
        }
        newUser.setId(UUID.randomUUID().toString());
        userList.add(newUser);
        return true;
    }

    public boolean changePassword(String email, String oldPassword, String newPassword) {
        for (User user : userList) {
            if (user.getEmail().equalsIgnoreCase(email) && user.getPassword().equals(oldPassword)) {
                user.setPassword(newPassword);
                return true;
            }
        }
        return false;
    }

    public User getCurrentUser() {
        String userId = prefs.getString("logged_user_id", null);
        if (userId == null) return null;
        
        for (User u : userList) {
            if (u.getId().equals(userId)) {
                return u;
            }
        }
        return null;
    }

    public List<User> getPatientsForDoctor(String doctorId) {
        List<User> patients = new ArrayList<>();
        for(User user : userList) {
            if("pacient".equals(user.getRole()) && doctorId.equals(user.getDoctorId())) {
                patients.add(user);
            }
        }
        return patients;
    }

    public void logout() {
        if (firebaseAuth != null) {
            firebaseAuth.signOut();
        }
        prefs.edit().clear().apply();
    }

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String error);
    }

    public boolean isLoggedIn() {
        return prefs.contains("logged_user_id");
    }
}
