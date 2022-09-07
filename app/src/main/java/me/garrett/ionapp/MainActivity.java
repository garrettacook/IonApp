package me.garrett.ionapp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import net.openid.appauth.connectivity.DefaultConnectionBuilder;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ForkJoinPool;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private AuthorizationService authService;
    private ActivityResultLauncher<Intent> authResultLauncher;
    private final @NotNull AuthState authState = new AuthState(Ion.SERVICE_CONFIG);

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
                Ion.SERVICE_CONFIG,
                Ion.CLIENT_ID,
                ResponseTypeValues.CODE, // the response_type value: we want a code
                Uri.parse("https://gcook.sites.tjhsst.edu/appauth") // the redirect URI to which the auth response is sent
        ).setScope(Ion.SCOPE).build();

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
            } else {
                assert exception != null;
                exception.printStackTrace();
                //TODO: Handle failure
            }
        });
    }

    private void updateDisplay() {
        System.out.println("updateDisplay");
        TextView textView = findViewById(R.id.auth_status);
        textView.setText("Updating...");
        authState.performActionWithFreshTokens(authService, ((accessToken, idToken, exception) -> {
            if (accessToken != null) {
                ForkJoinPool.commonPool().execute(() -> {
                    try {
                        HttpsURLConnection conn = (HttpsURLConnection) new URL(Ion.API_ROOT + "profile").openConnection();
                        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                        conn.setInstanceFollowRedirects(false);
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                            StringBuilder builder = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null)
                                builder.append(line);
                            JSONObject profile = new JSONObject(builder.toString());
                            String name = profile.getString("display_name");
                            runOnUiThread(() -> {
                                textView.setText("Logged in as " + name);
                                Snackbar.make(textView, "Logged in as " + name, 3000).show();
                            });
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        //TODO: Handle error
                    }
                });
            } else {
                assert exception != null;
                exception.printStackTrace();
            }
        }));
    }

}