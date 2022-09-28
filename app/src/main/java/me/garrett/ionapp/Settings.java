package me.garrett.ionapp;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class Settings {
    private Settings() {
    }

    public static final String SETTINGS_FILE = "me.garrett.ionapp.SETTINGS";
    public static final String BUS_ROUTE_KEY = "busRoute";

    public static @NonNull
    SharedPreferences getPreferences(@NonNull Context context) {
        return context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);
    }

}
