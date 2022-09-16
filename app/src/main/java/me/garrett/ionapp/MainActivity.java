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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import net.openid.appauth.AuthorizationService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import me.garrett.ionapp.api.Bus;
import me.garrett.ionapp.api.CheckScheduleWorker;
import me.garrett.ionapp.api.IonApi;

public class MainActivity extends AppCompatActivity {

    private static final String BUS_ROUTE_KEY = "busRoute";
    private static final String BUS_CHANNEL_ID = "me.garrett.ionapp.BUS_UPDATES";
    private static final int BUS_NOTIFICATION_ID = 100;

    private AuthorizationService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationChannel channel = new NotificationChannel(
                BUS_CHANNEL_ID, getString(R.string.bus_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        PeriodicWorkRequest checkScheduleRequest = new
                PeriodicWorkRequest.Builder(CheckScheduleWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "checkIonSchedule",
                ExistingPeriodicWorkPolicy.KEEP,
                checkScheduleRequest);

        if (!IonApi.getInstance(this).isAuthorized())
            startActivity(new Intent(this, LoginActivity.class));

        authService = new AuthorizationService(this);

        Spinner busSpinner = findViewById(R.id.busSpinner);
        IonApi.getInstance(this).getBusList(authService).thenAcceptAsync(busList -> runOnUiThread(() -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                    busList.stream().map(Bus::getRoute).collect(Collectors.toList()));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            busSpinner.setAdapter(adapter);

            String busRoute = getPreferences(MODE_PRIVATE).getString(BUS_ROUTE_KEY, null);
            if (busRoute != null)
                busSpinner.setSelection(adapter.getPosition(busRoute));
        }));

        busSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getPreferences(MODE_PRIVATE).edit()
                        .putString(BUS_ROUTE_KEY, (String) busSpinner.getSelectedItem()).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                getPreferences(MODE_PRIVATE).edit().remove(BUS_ROUTE_KEY).apply();
            }
        });

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(view -> {
            IonApi.getInstance(this).clearAuthState();
            startActivity(new Intent(this, LoginActivity.class));
        });

        Button findBusButton = findViewById(R.id.findBusButton);
        findBusButton.setOnClickListener(this::onFindBusClick);
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
        findBusLocation(route).thenAcceptAsync(
                optional -> {
                    if (optional.isPresent()) {
                        String message = getString(R.string.bus_status, route, optional.get());
                        sendBusArrivalNotification(route, optional.get());
                        runOnUiThread(() -> statusTextView.setText(message));
                    } else {
                        runOnUiThread(() -> statusTextView.setText(route + " is not in the bus depo."));
                    }
                }
        );
    }

    private void sendBusArrivalNotification(@NonNull String route, @NonNull String locationMessage) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, BUS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.bus_arrived, route))
                .setContentText(getString(R.string.generic_bus_status, locationMessage))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(this).notify(BUS_NOTIFICATION_ID, builder.build());
    }

    private @NonNull
    CompletableFuture<Optional<String>> findBusLocation(@NonNull String busRoute) {
        return IonApi.getInstance(this).getBusList(authService).thenApplyAsync(busList -> {
            for (Bus bus : busList) {
                if (bus.getRoute().equals(busRoute)) {
                    if (bus.getSpace() != null) {
                        return Optional.of(IonUtils.getBusLocationMessage(
                                IonUtils.getBusCoordinates(bus.getSpace()), busList));
                    }
                    break;
                }
            }
            return Optional.empty();
        });
    }

}