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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import me.garrett.ionapp.api.IonApi;
import me.garrett.ionapp.api.Signup;

public class EighthReminderReceiver extends BroadcastReceiver {

    private static final @NonNull
    String DATE_KEY = "date", BLOCKS_KEY = "blocks";

    public static @NonNull
    Intent createIntent(@NonNull Context context, @NonNull String date, @NonNull Set<Character> blocks) {
        StringBuilder builder = new StringBuilder(blocks.size());
        for (char blockLetter : blocks)
            builder.append(blockLetter);

        return new Intent(context, EighthReminderReceiver.class)
                .putExtra(DATE_KEY, date)
                .putExtra(BLOCKS_KEY, builder.toString());
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String date = intent.getStringExtra(DATE_KEY);
        if (date == null)
            throw new IllegalArgumentException("Date extra not set");

        String blockLetters = intent.getStringExtra(BLOCKS_KEY);
        if (blockLetters == null)
            throw new IllegalArgumentException("Blocks extra not set");

        Log.d("IonApp", "Checking signups...");
        checkSignups(context, date, blockLetters);

    }

    public static void checkSignups(@NonNull Context context, @NonNull String date, @NonNull String blockLetters) {
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
                                .putString(BLOCKS_KEY, blockLetters)
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

            String blockLetters = getInputData().getString(BLOCKS_KEY);
            if (blockLetters == null)
                throw new IllegalArgumentException("Blocks input not set");
            Set<Character> blocks = new HashSet<>(blockLetters.length());
            for (char blockLetter : blockLetters.toCharArray())
                blocks.add(blockLetter);

            AuthorizationService authService = new AuthorizationService(getApplicationContext());
            try {

                IonApi api = IonApi.getInstance(getApplicationContext());
                Map<Character, Signup> signups = api.getSignups(authService, date, blocks).get();

                Map<Character, Integer> blockIds = new HashMap<>(blocks.size());
                if (signups.size() != blocks.size()) {
                    JSONArray results = api.getBlocks(authService, date).get().getJSONArray("results");
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject blockJson = results.getJSONObject(i);
                        blockIds.put(blockJson.getString("block_letter").charAt(0), blockJson.getInt("id"));
                    }
                }

                for (char blockLetter : blocks) {
                    if (signups.containsKey(blockLetter)) {
                        Signup signup = signups.get(blockLetter);

                        //noinspection ConstantConditions
                        JSONObject activity = api.getBlockDetails(authService, signup.getBlockId()).get()
                                .getJSONObject("activities")
                                .getJSONObject(String.valueOf(signup.getActivityId()));

                        if (activity.getBoolean("cancelled")) {
                            Notifications.sendEighthSignupReminder(getApplicationContext(),
                                    signup.getBlockId(), blockLetter,
                                    activity.getString("name_with_flags_for_user"));
                        }

                    } else {
                        //noinspection ConstantConditions
                        Notifications.sendEighthSignupReminder(getApplicationContext(),
                                blockIds.get(blockLetter), blockLetter, null);
                    }
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
