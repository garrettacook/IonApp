package me.garrett.ionapp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private AuthorizationService authService;
    private ActivityResultLauncher<Intent> authResultLauncher;
    private final @NotNull AuthState authState = new AuthState(IonUtils.SERVICE_CONFIG);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authService = new AuthorizationService(this);
        authResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> onAuthResult(authService, result)
        );

        Button loginButton = findViewById(R.id.login_button);
        if (authState.isAuthorized()) {
            updateDisplay();
        } else {
            loginButton.setOnClickListener(view -> authorize());
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        authService.dispose();
    }

    public void authorize() {
        System.out.println("authorize");
        AuthorizationRequest authRequest = new AuthorizationRequest.Builder(
                IonUtils.SERVICE_CONFIG,
                IonUtils.CLIENT_ID,
                ResponseTypeValues.CODE, // the response_type value: we want a code
                Uri.parse("https://gcook.sites.tjhsst.edu/appauth") // the redirect URI to which the auth response is sent
        ).setScope(IonUtils.SCOPE).build();

        AuthorizationService authService = new AuthorizationService(this);
        authResultLauncher.launch(authService.getAuthorizationRequestIntent(authRequest));
    }

    private void onAuthResult(AuthorizationService authService, ActivityResult result) {
        System.out.println("onAuthResult");
        assert result.getData() != null;
        AuthorizationResponse response = AuthorizationResponse.fromIntent(result.getData());
        AuthorizationException exception = AuthorizationException.fromIntent(result.getData());
        authState.update(response, exception);
        if (response != null) { // success
            exchangeCode(authService, response.createTokenExchangeRequest());
        } else {
            assert exception != null;
            exception.printStackTrace();
            //TODO: Handle failure
        }
    }

    private void exchangeCode(AuthorizationService authService, TokenRequest tokenRequest) {
        System.out.println("exchangeCode");
        authService.performTokenRequest(tokenRequest, (response, exception) -> {
            authState.update(response, exception);
            if (response != null) {
                System.out.println("Saving authState");
                getSharedPreferences(getString(R.string.auth_file_key), Context.MODE_PRIVATE).edit()
                        .putString("authState", authState.jsonSerializeString()).apply();
                updateDisplay();
                showBusLocation("JT-102");
            } else {
                assert exception != null;
                exception.printStackTrace();
                //TODO: Handle failure
            }
        });
    }

    public static @NotNull String readString(@NotNull InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                builder.append(line);
            return builder.toString();
        }
    }

    private void handleConnectionError(@NotNull Exception exception) {
        exception.printStackTrace();
        Snackbar.make(findViewById(R.id.main_layout), exception.toString(), 3000).show();
    }

    private void updateDisplay() {
        System.out.println("updateDisplay");
        TextView textView = findViewById(R.id.auth_status);
        textView.setText("Updating...");
        executeQuery("profile", profile -> {
            try {
                String name = profile.getString("display_name");
                runOnUiThread(() -> {
                    //textView.setText("Logged in as " + name);
                    Snackbar.make(textView, "Logged in as " + name, 3000).show();
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void showBusLocation(@NotNull String busRoute) {
        executeQuery("bus", json -> {
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