package me.garrett.ionapp.api;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.openid.appauth.AuthorizationService;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import me.garrett.ionapp.Notifications;
import me.garrett.ionapp.Settings;

public class FindBusWorker extends Worker {

    private static final String UNIQUE_WORK_NAME = "FindBusWork";
    public static final String STOP_TIME = "stopTime";
    public static final Duration DURATION = Duration.ofMinutes(20);
    private static final int INTERVAL = 10000; // 10 seconds

    public FindBusWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static Operation start(@NonNull Context context, long stopMillis) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FindBusWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(new Data.Builder().putLong(STOP_TIME, stopMillis).build())
                .build();
        return WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request);
    }

    public static Operation cancel(@NonNull Context context) {
        return WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME);
    }

    @NonNull
    @Override
    public Result doWork() {

        long stopMillis = getInputData().getLong(STOP_TIME, 0);
        if (stopMillis == 0)
            throw new IllegalArgumentException("Stop time data not set");
        Instant stopTime = Instant.ofEpochMilli(stopMillis);

        AuthorizationService authService = new AuthorizationService(getApplicationContext());
        try {
            do {

                String route = Settings.getPreferences(getApplicationContext()).getString(Settings.BUS_ROUTE_KEY, null);
                if (route == null)
                    continue;

                try {
                    Log.d("IonApp", "Checking bus list...");
                    Optional<String> busLocation = IonApi.getInstance(getApplicationContext()).findBusLocation(authService, route).get();
                    if (busLocation.isPresent()) {
                        Notifications.sendBusArrivalNotification(getApplicationContext(), route, busLocation.get());
                        return Result.success();
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }

                SystemClock.sleep(INTERVAL);
            } while (!isStopped() && Instant.now().isBefore(stopTime));
            return Result.failure();

        } finally {
            authService.dispose();
        }
    }

}
