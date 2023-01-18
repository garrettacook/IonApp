package me.garrett.ionapp.api;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenResponse;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import javax.net.ssl.HttpsURLConnection;

import me.garrett.ionapp.IonUtils;

public class IonApi {

    private static final String AUTH_STATE_FILE = "me.garrett.ionapp.AUTH_STATE";
    private static final String AUTH_STATE_KEY = "authState";
    private static final String LAST_SUCCESS_KEY = "lastSuccess";
    public static final String AUTH_STATE_UPDATE = "me.garrett.ionapp.AUTH_STATE_UPDATE";

    public static final ZoneId ION_TIME_ZONE = ZoneId.of("America/New_York");
    public static final DateTimeFormatter TIME_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .toFormatter(Locale.ENGLISH);
    public static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .toFormatter(Locale.ENGLISH);

    private static @Nullable
    IonApi instance;

    public static @NonNull
    IonApi getInstance(@NonNull Context context) {
        if (instance == null)
            instance = new IonApi(context);
        return instance;
    }

    private final @NonNull
    Context context;
    private final @NonNull
    SharedPreferences preferences;
    private @NonNull
    AuthState authState;

    private IonApi(@NonNull Context context) {
        this.context = context.getApplicationContext();
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

    public @NonNull
    String getLastSuccess() {
        return preferences.getString(LAST_SUCCESS_KEY, "null");
    }

    public boolean isAuthorized() {
        return authState.isAuthorized();
    }

    public void clearAuthState() {
        preferences.edit().remove(AUTH_STATE_KEY).apply();
    }

    private void editAuthState() {
        preferences.edit().putString(AUTH_STATE_KEY, authState.jsonSerializeString()).apply();
        context.sendBroadcast(new Intent(AUTH_STATE_UPDATE));
    }

    public void update(@Nullable AuthorizationResponse authResponse, @Nullable AuthorizationException authException) {
        authState.update(authResponse, authException);
        editAuthState();
    }

    public void update(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException authException) {
        authState.update(tokenResponse, authException);
        editAuthState();
    }

    public @Nullable
    AuthorizationException getAuthorizationException() {
        return authState.getAuthorizationException();
    }

    @Nullable
    public String getRefreshToken() {
        return authState.getRefreshToken();
    }

    @Nullable
    public String getAccessToken() {
        return authState.getAccessToken();
    }

    @Nullable
    public Long getAccessTokenExpirationTime() {
        return authState.getAccessTokenExpirationTime();
    }

    public boolean getNeedsTokenRefresh() {
        return authState.getNeedsTokenRefresh();
    }

    public void setNeedsTokenRefresh(boolean needsTokenRefresh) {
        authState.setNeedsTokenRefresh(needsTokenRefresh);
        editAuthState();
    }

    public @NonNull
    String asJson() {
        return authState.jsonSerializeString();
    }

    public void performActionWithFreshTokens(@NonNull AuthorizationService authService, @NonNull AuthState.AuthStateAction action) {
        authState.performActionWithFreshTokens(authService, (accessToken, idToken, ex) -> {
            editAuthState(); // ENSURE SAVED STATE IS UP-TO-DATE
            if (authState.isAuthorized() && authState.getAuthorizationException() == null)
                preferences.edit().putString(LAST_SUCCESS_KEY, LocalDateTime.now().toString()).apply();
            action.execute(accessToken, idToken, ex);
        });
    }

    @FunctionalInterface
    public interface IOFunction<T, R> {
        R apply(T t) throws IOException, JSONException;
    }

    public @NonNull
    <T> CompletableFuture<T> connect(@NonNull AuthorizationService authService, @NonNull String endPoint, @NonNull IOFunction<HttpsURLConnection, T> function) {
        CompletableFuture<T> future = new CompletableFuture<>();
        performActionWithFreshTokens(authService, (accessToken, idToken, ex) -> {
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
    <T> CompletableFuture<T> get(@NonNull AuthorizationService authService, @NonNull String endPoint, @NonNull IOFunction<String, T> function) {
        return connect(authService, endPoint, conn -> {
            if (conn.getResponseCode() == 200) {
                String jsonString = IonUtils.readString(conn.getInputStream());
                return function.apply(jsonString);
            } else {
                throw new IOException(conn.getResponseCode() + " " + conn.getResponseMessage());
            }
        });
    }

    public @NonNull
    CompletableFuture<Schedule> getSchedule(@NonNull AuthorizationService authService, @NonNull Instant instant) {
        return get(authService, String.format("schedule/%tF", instant.atZone(ION_TIME_ZONE)), Schedule::fromRawJson);
    }

    public @NonNull
    CompletableFuture<List<Announcement>> getAnnouncements(@NonNull AuthorizationService authService, int page) {
        return get(authService, String.format(Locale.ROOT, "announcements?page=%d", page), Announcement::listFromRawJson);
    }

    public @NonNull
    CompletableFuture<List<Bus>> getBusList(@NonNull AuthorizationService authService) {
        return get(authService, "bus", Bus::listFromRawJson);
    }

    public @NonNull
    CompletableFuture<Optional<String>> findBusLocation(@NonNull AuthorizationService authService, @NonNull String busRoute) {
        return getBusList(authService).thenApplyAsync(busList -> {
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

    public @NonNull
    CompletableFuture<JSONArray> getSignups(@NonNull AuthorizationService authService) {
        return get(authService, "signups/user", JSONArray::new);
    }

    public @NotNull
    CompletableFuture<Optional<Signup>> getSignup(@NonNull AuthorizationService authService, @NonNull String date, char block) {
        return getSignups(authService).thenComposeAsync(signupsJsonArray -> {
            try {

                for (int i = 0; i < signupsJsonArray.length(); i++) {
                    JSONObject signupJson = signupsJsonArray.getJSONObject(i);

                    JSONObject blockJson = signupJson.getJSONObject("block");
                    if (blockJson.getString("date").equals(date) && blockJson.getString("block_letter").charAt(0) == block) {

                        Signup signup = new Signup(blockJson.getInt("id"), signupJson.getJSONObject("activity").getInt("id"));
                        return CompletableFuture.completedFuture(Optional.of(signup));

                    }

                }
                return CompletableFuture.completedFuture(Optional.empty());

            } catch (JSONException e) {
                CompletableFuture<Optional<Signup>> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        });
    }

    public @NonNull
    CompletableFuture<JSONObject> getBlockDetails(@NonNull AuthorizationService authService, int blockId) {
        return get(authService, String.format(Locale.ROOT, "blocks/%d", blockId), JSONObject::new);
    }

    public @NonNull
    CompletableFuture<JSONObject> getScheduledActivityDetails(@NonNull AuthorizationService authService, @NonNull Signup signup) {
        return getScheduledActivityDetails(authService, signup.getBlockId(), signup.getActivityId());
    }

    public @NonNull
    CompletableFuture<JSONObject> getScheduledActivityDetails(@NonNull AuthorizationService authService, int blockId, int activityId) {
        return getBlockDetails(authService, blockId).thenComposeAsync(json -> {
            try {

                JSONObject activityJson = json.getJSONObject("activities").getJSONObject(String.valueOf(activityId));
                return CompletableFuture.completedFuture(activityJson);

            } catch (JSONException e) {
                CompletableFuture<JSONObject> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        });
    }

}
