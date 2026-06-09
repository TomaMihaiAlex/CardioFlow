package com.example.cardioflow;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class CardioFlowApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // FirebaseApp.initializeApp(this) is usually called automatically by the 
            // Google Services plugin via the generated FirebaseInitProvider.
            FirebaseApp.initializeApp(this);
        } catch (Exception e) {
            // If google-services.json is missing or plugin is not applied, 
            // this might fail or the default app won't be initialized.
        }
    }
}
