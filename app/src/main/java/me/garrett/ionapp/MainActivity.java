package me.garrett.ionapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.Snackbar;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import me.garrett.ionapp.api.Bus;
import me.garrett.ionapp.api.CheckScheduleWorker;
import me.garrett.ionapp.api.FindBusWorker;
import me.garrett.ionapp.api.IonApi;

public class MainActivity extends AppCompatActivity {

    private static final String CHECK_SCHEDULE_UNIQUE_WORK_NAME = "CheckIonSchedule";

    private AuthorizationService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        NotificationChannel busChannel = new NotificationChannel(
                Notifications.BUS_CHANNEL_ID, getString(R.string.bus_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(busChannel);
        NotificationChannel testChannel = new NotificationChannel(
                Notifications.TEST_CHANNEL_ID, "Tests",
                NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(testChannel);

        PeriodicWorkRequest checkScheduleRequest = new
                PeriodicWorkRequest.Builder(CheckScheduleWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                CHECK_SCHEDULE_UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                checkScheduleRequest);

        // redirect to login activity if not authorized
        if (!IonApi.getInstance(this).isAuthorized())
            startActivity(new Intent(this, LoginActivity.class));

        authService = new AuthorizationService(this);

        Spinner busSpinner = findViewById(R.id.busSpinner);
        IonApi.getInstance(this).getBusList(authService).whenCompleteAsync((busList, ex) -> runOnUiThread(() -> {
            if (busList != null) {

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                        busList.stream().map(Bus::getRoute).collect(Collectors.toList()));
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                busSpinner.setAdapter(adapter);

                String busRoute = Settings.getPreferences(this).getString(Settings.BUS_ROUTE_KEY, null);
                if (busRoute != null)
                    busSpinner.setSelection(adapter.getPosition(busRoute));

            } else {
                AuthorizationException authException = IonApi.getInstance(this).getAuthorizationException();
                Snackbar.make(busSpinner, (authException != null ? authException : ex).toString(), 5000).show();
            }
        }));

        busSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Settings.getPreferences(MainActivity.this).edit()
                        .putString(Settings.BUS_ROUTE_KEY, (String) busSpinner.getSelectedItem()).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Settings.getPreferences(MainActivity.this).edit().remove(Settings.BUS_ROUTE_KEY).apply();
            }
        });

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(view -> {
            IonApi.getInstance(this).clearAuthState();
            startActivity(new Intent(this, LoginActivity.class));
        });

        Button findBusButton = findViewById(R.id.findBusButton);
        findBusButton.setOnClickListener(this::onFindBusClick);

        Button beginCheckingButton = findViewById(R.id.beginCheckingButton);
        Button cancelButton = findViewById(R.id.cancelButton);
        beginCheckingButton.setOnClickListener(view -> {
            beginCheckingButton.setEnabled(false);
            FindBusWorker.start(this);
        });
        cancelButton.setOnClickListener(view -> {
            FindBusWorker.cancel(this);
            beginCheckingButton.setEnabled(true);
        });

        Button resetWorkersButton = findViewById(R.id.resetWorkersButton);
        resetWorkersButton.setOnClickListener(view ->
                WorkManager.getInstance(this).cancelUniqueWork(CHECK_SCHEDULE_UNIQUE_WORK_NAME));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        authService.dispose();
    }

    private void onFindBusClick(View view) {
        TextView statusTextView = findViewById(R.id.busStatusTextView);
        statusTextView.setText(R.string.updating);

        String route = (String) ((Spinner) findViewById(R.id.busSpinner)).getSelectedItem();
        IonApi.getInstance(this).findBusLocation(authService, route).whenComplete(
                (optional, ex) -> {
                    if (ex == null) {

                        if (optional.isPresent()) {
                            String message = getString(R.string.bus_status, route, optional.get());
                            runOnUiThread(() -> statusTextView.setText(message));
                        } else {
                            runOnUiThread(() -> statusTextView.setText(getString(R.string.not_in_depo, route)));
                        }

                    } else {
                        runOnUiThread(() -> {
                            AuthorizationException authException = IonApi.getInstance(this).getAuthorizationException();
                            Snackbar.make(statusTextView, (authException != null ? authException : ex).toString(), 5000).show();
                        });
                    }
                }
        );
    }

}