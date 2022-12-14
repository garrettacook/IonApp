package me.garrett.ionapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public final class Notifications {
    private Notifications() {
    }

    public static final String BUS_CHANNEL_ID = "me.garrett.ionapp.BUS_UPDATES",
            TEST_CHANNEL_ID = "me.garrett.ionapp.TESTS",
            EIGHTH_CHANNEL_ID = "me.garrett.ionapp.EIGHTH";
    public static final int BUS_NOTIFICATION_ID = 100, EIGHTH_ACTIVITY_ID = 101;

    public static void sendBusArrivalNotification(@NonNull Context context, @NonNull String route, @NonNull String locationMessage) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Notifications.BUS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.bus_arrived, route))
                .setContentText(context.getString(R.string.generic_bus_status, locationMessage))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(context).notify(Notifications.BUS_NOTIFICATION_ID, builder.build());
    }

    public static void sendStatusNotification(@NonNull Context context, String status) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Notifications.TEST_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("IonApp Status")
                .setContentText(status)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        NotificationManagerCompat.from(context).notify(0, builder.build());
    }

    public static void sendEighthNotification(@NonNull Context context, @NonNull String activity, @NonNull String room) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Notifications.EIGHTH_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(activity)
                .setContentText(context.getString(R.string.activity_location, room))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(context).notify(EIGHTH_ACTIVITY_ID, builder.build());
    }

}
