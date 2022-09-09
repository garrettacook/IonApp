package me.garrett.ionapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenResponse;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import javax.net.ssl.HttpsURLConnection;

public class IonApi {

    private static final String AUTH_STATE_FILE = "me.garrett.ionapp.AUTH_STATE";
    private static final String AUTH_STATE_KEY = "authState";

    private static @Nullable IonApi instance;

    public static @NonNull IonApi getInstance(@NonNull Context context) {
        if (instance == null)
            instance = new IonApi(context);
        return instance;
    }

    private final @NonNull SharedPreferences preferences;
    private @NonNull AuthState authState;

    private IonApi(@NonNull Context context) {
        preferences = context.getSharedPreferences(AUTH_STATE_FILE, Context.MODE_PRIVATE);
        String authStateString = preferences.getString(AUTH_STATE_KEY, null);
        if (authStateString != null) {
            try {
                authState = AuthState.jsonDeserialize(authStateString);
                return;
            } catch (JSONException e) {
                Log.w(getClass().getSimpleName(), "Failed to deserialize auth state string!");
                e.printStackTrace();
            }
        }
        authState = new AuthState();
    }

    private void editAuthState() {
        preferences.edit().putString(AUTH_STATE_KEY, authState.jsonSerializeString()).apply();
    }

    public void update(@Nullable AuthorizationResponse authResponse, @Nullable AuthorizationException authException) {
        authState.update(authResponse, authException);
        editAuthState();
    }

    public void update(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException authException) {
        authState.update(tokenResponse, authException);
        editAuthState();
    }

    private static void query(@NonNull String endPoint, @NonNull String method, @NonNull String accessToken, @NonNull Consumer<HttpsURLConnection> connectionConsumer) {
        ForkJoinPool.commonPool().execute(() -> {
            try {
                HttpsURLConnection conn = (HttpsURLConnection) new URL(IonUtils.API_ROOT + endPoint).openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                connectionConsumer.accept(conn);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void performActionWithFreshTokens(@NonNull AuthorizationService authService, @NonNull AuthState.AuthStateAction action) {
        authState.performActionWithFreshTokens(authService, action);
    }

    private void query(@NotNull String endPoint, @NotNull String method, Consumer<HttpsURLConnection> callback) {
        if (!authState.isAuthorized())
            throw new IllegalStateException("Not authorized");
        authState.performActionWithFreshTokens(authService, ((accessToken, idToken, exception) -> {
            if (accessToken != null) {
                ForkJoinPool.commonPool().execute(() -> {
                    try {
                        HttpsURLConnection conn = (HttpsURLConnection) new URL(IonUtils.API_ROOT + endPoint).openConnection();
                        conn.setRequestMethod(method);
                        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                        callback.accept(conn);
                    } catch (IOException e) {
                        handleConnectionError(e);
                    }
                });
            } else {
                assert exception != null;
                handleConnectionError(exception);
            }
        }));
    }

}
