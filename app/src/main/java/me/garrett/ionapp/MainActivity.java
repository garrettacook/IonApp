package me.garrett.ionapp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import net.openid.appauth.AuthorizationService;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Optional;

public class MainActivity extends AppCompatActivity {

    private AuthorizationService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authService = new AuthorizationService(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        authService.dispose();
    }

    private void showBusLocation(@NotNull String busRoute) {
        IonApi.getInstance(this) "bus", json -> {
            try {
                JSONArray busArray = json.getJSONArray("results");
                Optional<JSONObject> bus = IonUtils.findBus(busArray, b -> b.getString("route_name").equals(busRoute), b -> b);
                if (bus.isPresent()) {
                    String location = IonUtils.getBusLocationMessage(
                            IonUtils.getBusCoordinates(bus.get().getString("space")),
                            busArray
                    );
                    runOnUiThread(() -> {
                        TextView textView = findViewById(R.id.auth_status);
                        textView.setText(busRoute + " is " + location);
                    });
                } else {
                    //TODO: Handle doesn't exist
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

}