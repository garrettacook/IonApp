package me.garrett.ionapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import java.time.Instant;

import me.garrett.ionapp.api.FindBusWorker;

public class StartFindBusWorkerReceiver extends BroadcastReceiver {

    public static @NonNull
    Intent createIntent(@NonNull Context context, @NonNull Instant stopTime) {
        return new Intent(context, StartFindBusWorkerReceiver.class)
                .putExtra(FindBusWorker.STOP_TIME, stopTime.toEpochMilli());
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        long stopMillis = intent.getLongExtra(FindBusWorker.STOP_TIME, 0);
        if (stopMillis == 0)
            throw new IllegalArgumentException("Stop time extra not set");

        Log.d("IonApp", "Starting find bus worker");
        Notifications.sendStatusNotification(context, "Starting find bus worker");
        FindBusWorker.start(context, stopMillis);
    }

}