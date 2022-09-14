package me.garrett.ionapp.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import javax.net.ssl.HttpsURLConnection;

import me.garrett.ionapp.IonUtils;

public class IonApi {

    private static final String AUTH_STATE_FILE = "me.garrett.ionapp.AUTH_STATE";
    private static final String AUTH_STATE_KEY = "authState";

    private static @Nullable
    IonApi instance;

    public static @NonNull
    IonApi getInstance(@NonNull Context context) {
        if (instance == null)
            instance = new IonApi(context);
        return instance;
    }

    private final @NonNull
    SharedPreferences preferences;
    private @NonNull
    AuthState authState;

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

    public boolean isAuthorized() {
        return authState.isAuthorized();
    }

    public void clearAuthState() {
        preferences.edit().remove(AUTH_STATE_KEY).apply();
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

    @FunctionalInterface
    public interface IOFunction<T, R> {
        R apply(T t) throws IOException, JSONException;
    }

    public @NonNull
    <T> CompletableFuture<T> connect(@NonNull AuthorizationService authService, @NonNull String endPoint, @NonNull IOFunction<HttpsURLConnection, T> function) {
        CompletableFuture<T> future = new CompletableFuture<>();
        authState.performActionWithFreshTokens(authService, (accessToken, idToken, ex) -> {
            if (accessToken != null) {
                ForkJoinPool.commonPool().execute(() -> {
                    try {
                        HttpsURLConnection conn = (HttpsURLConnection) new URL(IonUtils.API_ROOT + endPoint).openConnection();
                        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                        conn.setRequestProperty("Accept", "application/json");
                        future.complete(function.apply(conn));
                    } catch (IOException | JSONException e) {
                        future.completeExceptionally(e);
                    }
                });
            } else {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    public @NonNull
    <T> CompletableFuture<T> get(@NonNull AuthorizationService authService, @NonNull String endPoint, @NonNull IOFunction<JSONObject, T> function) {
        return connect(authService, endPoint, conn -> {
            if (conn.getResponseCode() == 200) {
                String jsonString = IonUtils.readString(conn.getInputStream());
                return function.apply(new JSONObject(jsonString));
            } else {
                throw new IOException(conn.getResponseCode() + " " + conn.getResponseMessage());
            }
        });
    }

    public @NonNull
    CompletableFuture<List<Bus>> getBusList(@NonNull AuthorizationService authService) {
        return get(authService, "bus", json -> Bus.listFromJson(json.getJSONArray("results")));
    }

}
