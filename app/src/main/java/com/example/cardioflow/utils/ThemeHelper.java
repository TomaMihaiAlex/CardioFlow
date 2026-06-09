package com.example.cardioflow.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
        int theme = prefs.getInt(AppConstants.KEY_THEME, AppConstants.THEME_STANDARD);
        
        switch (theme) {
            case AppConstants.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case AppConstants.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
