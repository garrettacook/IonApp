package me.garrett.ionapp;

import android.net.Uri;

import androidx.annotation.NonNull;

import net.openid.appauth.AuthorizationServiceConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import me.garrett.ionapp.api.Bus;

public final class IonUtils {
    private IonUtils() {
    }

    public static final String
            BASE_URL = "https://ion.tjhsst.edu/",
            CLIENT_ID = "kWQhxJDH8KZkd6gIiD7qI40KQhHl7pXngxvSPBy0",
            SCOPE = "read",
            AUTHORIZATION_ENDPOINT = BASE_URL + "oauth/authorize",
            TOKEN_ENDPOINT = BASE_URL + "oauth/token",
            API_ROOT = BASE_URL + "api/";

    public static final AuthorizationServiceConfiguration SERVICE_CONFIG =
            new AuthorizationServiceConfiguration(
                    Uri.parse(AUTHORIZATION_ENDPOINT),
                    Uri.parse(TOKEN_ENDPOINT));

    public static @NonNull
    String readString(@NonNull InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                builder.append(line);
            return builder.toString();
        }
    }

    public static @NonNull
    int[] getBusCoordinates(@NonNull String space) {
        if (space.length() >= 2 && space.charAt(0) == '_') {
            int number = Integer.parseInt(space.substring(1));

            if (number <= 9 || number >= 31) {
                // curb

                if (number <= 9)
                    return new int[]{0, 3 - number};
                else if (number <= 40)
                    return new int[]{1, number - 37};
                else if (number == 41)
                    return new int[]{0, 3};

            } else {
                // diagonals

                if (number <= 22)
                    return new int[]{2, 14 - number};
                else if (number <= 30)
                    return new int[]{3, 27 - number};

            }

        }
        throw new IllegalArgumentException("Unknown space format: " + space);
    }

    public static @NonNull
    String getBusSpace(int row, int position) {
        int number;
        if (row == 0)
            number = position == 3 ? 41 : 3 - position;
        else if (row == 1)
            number = 37 + position;
        else if (row == 2)
            number = 14 - position;
        else if (row == 3)
            number = 27 - position;
        else
            throw new IllegalArgumentException("Unknown row: " + row);
        return "_" + number;
    }

    public static @NonNull
    int[] getBusRowEnds(int row) {
        if (row <= 1)
            return new int[]{-6, 3};
        else if (row == 2)
            return new int[]{-8, 4};
        else if (row == 3)
            return new int[]{-3, 4};
        else
            throw new IllegalArgumentException("Unknown row: " + row);
    }

    public static @NonNull
    String getBusLocationMessage(int[] coords, @NonNull List<Bus> busList) {
        return getBusLocationMessage(coords[0], coords[1], busList);
    }

    public static @NonNull
    String getBusLocationMessage(int row, int position, @NonNull List<Bus> busList) {
        String message = (row <= 1 ? "by the curb" : "in the lot")
                + " on the " + (position < 0 ? "left" : "right");

        int[] ends = getBusRowEnds(Math.min(row, 2));
        if (position == ends[0] || position == ends[1])
            message += " end";

        if (row == 1 || row == 3) {
            String aheadSpace = getBusSpace(row - 1, position);
            for (Bus bus : busList) {
                if (aheadSpace.equals(bus.getSpace())) {
                    message += ", behind " + bus.getRoute();
                    break;
                }
            }
        }

        return message;
    }

}
