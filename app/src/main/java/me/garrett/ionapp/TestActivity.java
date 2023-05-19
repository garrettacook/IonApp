package me.garrett.ionapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.Snackbar;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

import me.garrett.ionapp.api.Bus;
import me.garrett.ionapp.api.CheckScheduleWorker;
import me.garrett.ionapp.api.FindBusWorker;
import me.garrett.ionapp.api.IonApi;

public class TestActivity extends AppCompatActivity {

    private AuthorizationService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

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
                Settings.getPreferences(TestActivity.this).edit()
                        .putString(Settings.BUS_ROUTE_KEY, (String) busSpinner.getSelectedItem()).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Settings.getPreferences(TestActivity.this).edit().remove(Settings.BUS_ROUTE_KEY).apply();
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
            FindBusWorker.start(this, Instant.now().plus(FindBusWorker.DURATION).toEpochMilli());
        });
        cancelButton.setOnClickListener(view -> {
            FindBusWorker.cancel(this);
            beginCheckingButton.setEnabled(true);
        });

        Button resetWorkersButton = findViewById(R.id.resetWorkersButton);
        resetWorkersButton.setOnClickListener(view ->
                WorkManager.getInstance(this).cancelUniqueWork(MainActivity.CHECK_SCHEDULE_UNIQUE_WORK_NAME));

        Button forceRefreshButton = findViewById(R.id.forceRefreshButton);
        forceRefreshButton.setOnClickListener(view -> IonApi.getInstance(this).setNeedsTokenRefresh(true));

        Button resetHistoryButton = findViewById(R.id.resetHistoryButton);
        resetHistoryButton.setOnClickListener(view -> CheckScheduleWorker.clearHistory(this));

        Button testButton = findViewById(R.id.testButton);
        testButton.setOnClickListener(view -> {
            DebugUtils.writeLine(this, "authlog.txt", "Test button pressed");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        authService.dispose();
    }

    private static @Nullable String censor(@Nullable String token) {
        if (token == null)
            return null;

        StringBuilder builder = new StringBuilder(token);
        for (int i = 3; i < builder.length(); i++)
            builder.replace(i, i + 1, "*");
        return builder.toString();
    }

    private void updateAuthStateTextView() {
        IonApi api = IonApi.getInstance(this);

        TextView authStateAccessTokenView = findViewById(R.id.authStateAccessTokenView);
        TextView authStateRefreshTokenView = findViewById(R.id.authStateRefreshTokenView);
        TextView authStateExpiresAtView = findViewById(R.id.authStateExpiresAtView);
        TextView authStateRefreshView = findViewById(R.id.authStateRefreshView);

        authStateAccessTokenView.setText(censor(api.getAccessToken()));
        authStateRefreshTokenView.setText(censor(api.getRefreshToken()));
        authStateExpiresAtView.setText(Optional.ofNullable(api.getAccessTokenExpirationTime()).map(Instant::ofEpochMilli).map(Instant::toString).orElse("null"));
        authStateRefreshView.setText(String.valueOf(api.getNeedsTokenRefresh()));
    }

    private BroadcastReceiver authStateUpdateReceiver;

    @Override
    protected void onResume() {
        super.onResume();
        updateAuthStateTextView();
        authStateUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateAuthStateTextView();
            }
        };
        registerReceiver(authStateUpdateReceiver, new IntentFilter(IonApi.AUTH_STATE_UPDATE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(authStateUpdateReceiver);
        authStateUpdateReceiver = null;
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