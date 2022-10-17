package me.garrett.ionapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import me.garrett.ionapp.api.FindBusWorker;

public class StartFindBusWorkerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("IonApp", "Starting find bus worker");
        Notifications.sendStatusNotification(context, "Starting find bus worker");
        FindBusWorker.start(context);
    }

}