package me.garrett.ionapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import me.garrett.ionapp.api.FindBusWorker;

public class StartFindBusWorkerService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("IonApp", "Starting find bus worker");
        Notifications.sendStatusNotification(getApplicationContext(), "Starting find bus worker");
        FindBusWorker.start(getApplicationContext());
        stopSelf();
        return START_NOT_STICKY;
    }

}