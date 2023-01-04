package me.garrett.ionapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.openid.appauth.AuthorizationService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import me.garrett.ionapp.api.IonApi;
import me.garrett.ionapp.api.Signup;

public class EighthTransitionReceiver extends BroadcastReceiver {

    private static final @NonNull
    String DATE_KEY = "date", BLOCK_KEY = "block";

    public static @NonNull
    Intent createIntent(@NonNull Context context, @NonNull String date, char block) {
        return new Intent(context, EighthTransitionReceiver.class)
                .putExtra(DATE_KEY, date)
                .putExtra(BLOCK_KEY, block);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String date = intent.getStringExtra(DATE_KEY);
        if (date == null)
            throw new IllegalArgumentException("Date extra not set");

        char block = intent.getCharExtra(BLOCK_KEY, ' ');
        if (block == ' ')
            throw new IllegalArgumentException("Block extra not set");

        Log.d("IonApp", "Checking signup for " + block + " block...");
        checkSignup(context, date, block);

    }

    public static void checkSignup(@NonNull Context context, @NonNull String date, char block) {
        WorkManager workManager = WorkManager.getInstance(context);
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SignupCheckWorker.class)
                .setConstraints(
                        new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                )
                .setInputData(
                        new Data.Builder()
                                .putString(DATE_KEY, date)
                                .putString(BLOCK_KEY, String.valueOf(block))
                                .build()
                ).build();
        workManager.enqueue(workRequest);
    }

    public static class SignupCheckWorker extends Worker {

        public SignupCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {

            String date = getInputData().getString(DATE_KEY);
            if (date == null)
                throw new IllegalArgumentException("Date input not set");

            String blockString = getInputData().getString(BLOCK_KEY);
            if (blockString == null)
                throw new IllegalArgumentException("Block input not set");
            char block = blockString.charAt(0);

            AuthorizationService authService = new AuthorizationService(getApplicationContext());
            try {

                IonApi api = IonApi.getInstance(getApplicationContext());
                Optional<Signup> signup = api.getSignup(authService, date, block).get();
                if (signup.isPresent()) {

                    JSONObject json = api.getScheduledActivityDetails(authService, signup.get()).get();
                    String activity = json.getString("name_with_flags_for_user");

                    JSONArray roomsArray = json.getJSONArray("rooms");
                    List<String> rooms = new ArrayList<>(roomsArray.length());
                    for (int i = 0; i < roomsArray.length(); i++)
                        rooms.add(roomsArray.getString(i));

                    Notifications.sendEighthNotification(getApplicationContext(),
                            activity, String.join(", ", rooms));
                }
                return Result.success();

            } catch (ExecutionException | InterruptedException | JSONException e) {
                e.printStackTrace();
                return Result.failure();
            } finally {
                authService.dispose();
            }

        }

    }

}
