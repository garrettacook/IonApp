package me.garrett.ionapp;

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

import net.openid.appauth.AuthorizationService;

import java.util.stream.Collectors;

import me.garrett.ionapp.api.Bus;
import me.garrett.ionapp.api.IonApi;

public class MainActivity extends AppCompatActivity {

    private static final String BUS_ROUTE_KEY = "busRoute";

    private AuthorizationService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        Button findBusButton = findViewById(R.id.findBusButton);
        findBusButton.setOnClickListener(view -> findBusLocation((String) busSpinner.getSelectedItem()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        authService.dispose();
    }

    private void findBusLocation(@NonNull String busRoute) {
        TextView statusTextView = findViewById(R.id.busStatusTextView);
        statusTextView.setText(R.string.updating);

        IonApi.getInstance(this).getBusList(authService).thenAcceptAsync(busList -> {
            for (Bus bus : busList) {
                if (bus.getRoute().equals(busRoute)) {
                    String message = "not in the bus depo";
                    if (bus.getSpace() != null) {
                        message = IonUtils.getBusLocationMessage(
                                IonUtils.getBusCoordinates(bus.getSpace()), busList);
                    }

                    final String finalMessage = message;
                    runOnUiThread(() ->
                            statusTextView.setText(getString(R.string.bus_status, bus.getRoute(), finalMessage))
                    );
                    return;
                }
            }
        });
    }

}