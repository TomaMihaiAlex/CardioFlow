package com.example.cardioflow.auth;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.cardioflow.models.User;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuthManager {
    private static AuthManager instance;
    private Context context;
    private List<User> userList;
    private User currentUser;
    private SharedPreferences prefs;

    private AuthManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        loadUsersFromAssets();
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }

    private void loadUsersFromAssets() {
        try {
            InputStream is = context.getAssets().open("users.json");
            InputStreamReader reader = new InputStreamReader(is);
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<User>>(){}.getType();
            userList = gson.fromJson(reader, type);
        } catch (Exception e) {
            e.printStackTrace();
            userList = new ArrayList<>();
        }
    }

    // Salvează utilizatorii înapoi în fișier (opțional, pentru înregistrare)
    private void saveUsersToAssets() {
        // În realitate, pentru scriere în assets nu se poate direct;
        // pentru prototip, păstrăm modificările doar în memorie.
        // Dacă dorești persistare, poți scrie în internal storage.
    }

    public boolean login(String email, String password) {
        for (User user : userList) {
            if (user.getEmail().equals(email) && user.getPassword().equals(password)) {
                currentUser = user;
                // Salvează token (simulat) și email în SharedPreferences
                prefs.edit().putString("logged_user_id", user.getId()).apply();
                prefs.edit().putString("logged_user_email", user.getEmail()).apply();
                prefs.edit().putString("logged_user_role", user.getRole()).apply();
                return true;
            }
        }
        return false;
    }

    public boolean register(User newUser) {
        // Verifică dacă email există deja
        for (User u : userList) {
            if (u.getEmail().equals(newUser.getEmail())) {
                return false;
            }
        }
        newUser.setId(UUID.randomUUID().toString());
        userList.add(newUser);
        // În mod normal, ar trebui salvat în fișier, dar pentru demo rămâne în memorie
        return true;
    }

    public boolean changePassword(String email, String oldPassword, String newPassword) {
        for (User user : userList) {
            if (user.getEmail().equals(email) && user.getPassword().equals(oldPassword)) {
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
                currentUser = u;
                return u;
            }
        }
        return null;
    }

    public void logout() {
        prefs.edit().clear().apply();
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return prefs.contains("logged_user_id");
    }
}