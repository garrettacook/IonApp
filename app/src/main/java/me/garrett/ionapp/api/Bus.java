package me.garrett.ionapp.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Bus {

    public enum Status {
        ON_TIME("o"), ARRIVED("a"), DELAYED("d");

        private final @NonNull
        String string;

        Status(@NonNull String string) {
            this.string = string;
        }

        public static @NonNull
        Status fromString(@NonNull String string) {
            for (Status status : values()) {
                if (status.string.equals(string))
                    return status;
            }
            throw new IllegalArgumentException("Unknown status string: " + string);
        }

    }

    private final int id;
    private final @NonNull
    String route;
    private final @Nullable
    String space;
    private final @Nullable
    String busNumber;
    private final @NonNull
    Status status;

    protected Bus(int id, @NonNull String route, @Nullable String space, @Nullable String busNumber, @NonNull Status status) {
        this.id = id;
        this.route = route;
        this.space = space;
        this.busNumber = busNumber;
        this.status = status;
    }

    public static @NonNull
    Bus fromJson(@NonNull JSONObject json) throws JSONException {
        int id = json.getInt("id");
        String route = json.getString("route_name");
        String space = json.getString("space");
        String busNumber = json.getString("bus_number");
        String status = json.getString("status");
        return new Bus(id, route,
                !space.isEmpty() ? space : null,
                !busNumber.isEmpty() ? busNumber : null,
                Status.fromString(status)
        );
    }

    public static @NonNull
    List<Bus> listFromRawJson(@NonNull String json) throws JSONException {
        return listFromJson(new JSONObject(json).getJSONArray("results"));
    }

    public static @NonNull
    List<Bus> listFromJson(@NonNull JSONArray jsonArray) throws JSONException {
        List<Bus> list = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++)
            list.add(fromJson(jsonArray.getJSONObject(i)));
        return list;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String getRoute() {
        return route;
    }

    @Nullable
    public String getSpace() {
        return space;
    }

    @Nullable
    public String getBusNumber() {
        return busNumber;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

}
