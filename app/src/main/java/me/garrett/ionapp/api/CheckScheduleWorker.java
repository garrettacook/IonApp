package me.garrett.ionapp.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.openid.appauth.AuthorizationService;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

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
            Schedule schedule = api.getSchedule(authService, Instant.now()).get();


        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
