package me.garrett.ionapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;

import java.time.Instant;
import java.util.Optional;

import me.garrett.ionapp.api.IonApi;

public class LoginActivity extends AppCompatActivity {

    private static final AuthorizationServiceConfiguration SERVICE_CONFIG =
            new AuthorizationServiceConfiguration(
                    Uri.parse(IonUtils.AUTHORIZATION_ENDPOINT),
                    Uri.parse(IonUtils.TOKEN_ENDPOINT));

    private AuthorizationService authService;
    private ActivityResultLauncher<Intent> authResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authService = new AuthorizationService(this);
        authResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onAuthResult);

        Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(view -> authorize());

        updateAuthStateTextView();
    }

    @SuppressLint("SetTextI18n")
    private void updateAuthStateTextView() {
        IonApi api = IonApi.getInstance(this);

        TextView authStateView = findViewById(R.id.loginAuthStateView);

        authStateView.setText(api.getAccessToken() + "\n"
                + api.getRefreshToken() + "\n"
                + Optional.ofNullable(api.getAccessTokenExpirationTime()).map(Instant::ofEpochMilli).map(Instant::toString).orElse("null"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        authService.dispose();
    }

    private void authorize() {
        AuthorizationRequest authRequest = new AuthorizationRequest.Builder(
                SERVICE_CONFIG,
                IonUtils.CLIENT_ID,
                ResponseTypeValues.CODE, // the response_type value: we want a code
                Uri.parse("https://gcook.sites.tjhsst.edu/appauth") // the redirect URI to which the auth response is sent
        ).setScope(IonUtils.SCOPE).build();

        authResultLauncher.launch(authService.getAuthorizationRequestIntent(authRequest));
    }

    private void onAuthResult(ActivityResult result) {
        assert result.getData() != null;
        AuthorizationResponse response = AuthorizationResponse.fromIntent(result.getData());
        AuthorizationException exception = AuthorizationException.fromIntent(result.getData());
        IonApi.getInstance(this).update(response, exception);
        if (response != null) { // success
            exchangeCode(response.createTokenExchangeRequest());
        } else {
            assert exception != null;
            handleConnectionError(exception);
        }
    }

    private void exchangeCode(TokenRequest tokenRequest) {
        authService.performTokenRequest(tokenRequest, (response, exception) -> {
            IonApi.getInstance(this).update(response, exception);
            if (response != null) {
                startActivity(new Intent(this, TestActivity.class));
            } else {
                assert exception != null;
                handleConnectionError(exception);
            }
        });
    }

    private void handleConnectionError(@NonNull Exception exception) {
        exception.printStackTrace();
        Snackbar.make(findViewById(R.id.loginButton), exception.toString(), 3000).show();
    }

}