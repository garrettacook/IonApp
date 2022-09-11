package me.garrett.ionapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

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
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;

import me.garrett.ionapp.api.IonApi;

public class LoginActivity extends AppCompatActivity {

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        authService.dispose();
    }

    private void authorize() {
        AuthorizationRequest authRequest = new AuthorizationRequest.Builder(
                IonUtils.SERVICE_CONFIG,
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
                startActivity(new Intent(this, MainActivity.class));
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