package me.garrett.ionapp.api;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.openid.appauth.AuthorizationService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;

import me.garrett.ionapp.StartFindBusWorkerService;

public class CheckScheduleWorker extends Worker {

    public CheckScheduleWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public @NonNull
    Result doWork() {
        AuthorizationService authService = new AuthorizationService(getApplicationContext());
        IonApi api = IonApi.getInstance(getApplicationContext());

        try {
            Log.d("IonApp", "Checking schedule...");
            Schedule schedule = api.getSchedule(authService, Instant.now()).get();
            Instant checkTime = schedule.getEnd().minus(5, ChronoUnit.MINUTES);

            Log.d("IonApp", "Scheduling alarm for " + checkTime);
            Intent intent = new Intent(getApplicationContext(), StartFindBusWorkerService.class);
            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = getApplicationContext().getSystemService(AlarmManager.class);
            alarmManager.cancel(pendingIntent); // avoid duplicate alarms
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkTime.toEpochMilli(), pendingIntent);
            return Result.success();

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return Result.retry();
        }
    }

}
