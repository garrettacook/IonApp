package me.garrett.ionapp.api;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.openid.appauth.AuthorizationService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import me.garrett.ionapp.DebugUtils;
import me.garrett.ionapp.EighthTransitionReceiver;
import me.garrett.ionapp.Notifications;
import me.garrett.ionapp.StartFindBusWorkerReceiver;

public class CheckScheduleWorker extends Worker {

    private static final String HISTORY_FILE = "me.garrett.ionapp.CHECK_HISTORY",
            LAST_SCHEDULE_DATE_KEY = "lastScheduleDate",
            LAST_CHECK_TIME = "lastCheckTime";

    public CheckScheduleWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static boolean clearHistory(@NonNull Context context) {
        return context.deleteSharedPreferences(HISTORY_FILE);
    }

    @Override
    public @NonNull
    Result doWork() {
        AuthorizationService authService = new AuthorizationService(getApplicationContext());
        IonApi api = IonApi.getInstance(getApplicationContext());

        try {
            Log.d("IonApp", "Checking schedule...");

            Schedule schedule = api.getSchedule(authService, Instant.now()).get();
            if (schedule.isEmpty())
                return Result.success();

            Instant checkTime = schedule.getEnd().minus(5, ChronoUnit.MINUTES);
            if (checkTime.isBefore(Instant.now()))
                return Result.success();

            AlarmManager alarmManager = getApplicationContext().getSystemService(AlarmManager.class);

            {
                Log.d("IonApp", "Scheduling alarm for " + checkTime);
                Intent intent = new Intent(getApplicationContext(), StartFindBusWorkerReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

                alarmManager.cancel(pendingIntent); // avoid duplicate alarms
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkTime.toEpochMilli(), pendingIntent);
            }

            String date = schedule.getDate().format(IonApi.DATE_FORMAT);
            SharedPreferences history = getApplicationContext().getSharedPreferences(HISTORY_FILE, Context.MODE_PRIVATE);
            if (!date.equals(history.getString(LAST_SCHEDULE_DATE_KEY, null))) {

                Map<Character, Instant> transitionTimes = schedule.getEighthTransitionTimes();
                for (Map.Entry<Character, Instant> entry : transitionTimes.entrySet()) {
                    char block = entry.getKey();
                    Instant instant = entry.getValue();

                    Intent intent = EighthTransitionReceiver.createIntent(getApplicationContext(), date, block);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), block, intent, PendingIntent.FLAG_IMMUTABLE);

                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, instant.toEpochMilli(), pendingIntent);
                    Log.d("IonApp", "Scheduled " + block + " block alarm for " + instant);
                }

                Notifications.sendStatusNotification(getApplicationContext(),
                        "Scheduled alarms for blocks: " +
                                transitionTimes.keySet().stream().map(Object::toString).collect(Collectors.joining(", "))
                );

                history.edit().putString(LAST_SCHEDULE_DATE_KEY, date).apply();
            }

            String lastCheckTimeString = history.getString(LAST_CHECK_TIME, null);
            Instant lastCheckTime = lastCheckTimeString == null ? null : Instant.parse(lastCheckTimeString);

            if (lastCheckTime != null) {
                for (Announcement announcement : api.getAnnouncements(authService, 1).get()) {
                    if (announcement.getAdded().isAfter(lastCheckTime)) {
                        Notifications.sendAnnouncementNotification(getApplicationContext(), announcement);
                    } else if (!announcement.isPinned()) {
                        break;
                    }
                }
            }
            history.edit().putString(LAST_CHECK_TIME, Instant.now().toString()).apply();

            return Result.success();

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            DebugUtils.writeLine(getApplicationContext(), "authlog.txt", "Check Failure: " + e);
            Notifications.sendStatusNotification(getApplicationContext(), e.getClass().getSimpleName() + ": " + e.getMessage());
            return Result.retry();
        }
    }

}
