package me.garrett.ionapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import me.garrett.ionapp.api.Announcement;

public final class Notifications {
    private Notifications() {
    }

    public static final String BUS_CHANNEL_ID = "me.garrett.ionapp.BUS_UPDATES",
            TEST_CHANNEL_ID = "me.garrett.ionapp.TESTS",
            EIGHTH_CHANNEL_ID = "me.garrett.ionapp.EIGHTH",
            ANNOUNCEMENTS_CHANNEL_ID = "me.garrett.ionapp.ANNOUNCEMENTS";
    public static final int BUS_NOTIFICATION_ID = 100, EIGHTH_ACTIVITY_ID = 101;

    public static @NonNull PendingIntent getWebLinkPendingIntent(@NonNull Context context, @NonNull String path) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(IonUtils.BASE_URL + path));
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    public static void sendBusArrivalNotification(@NonNull Context context, @NonNull String route, @NonNull String locationMessage) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, BUS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.bus_arrived, route))
                .setContentText(context.getString(R.string.generic_bus_status, locationMessage))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(getWebLinkPendingIntent(context, "bus"))
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(BUS_NOTIFICATION_ID, builder.build());
    }

    public static void sendStatusNotification(@NonNull Context context, String status) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TEST_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("IonApp Status")
                .setContentText(status)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        NotificationManagerCompat.from(context).notify(0, builder.build());
    }

    public static void sendEighthNotification(@NonNull Context context, @NonNull String activity, @NonNull String room) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, EIGHTH_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(activity)
                .setContentText(context.getString(R.string.activity_location, room))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(getWebLinkPendingIntent(context, "eighth/glance"))
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(EIGHTH_ACTIVITY_ID, builder.build());
    }

    public static void sendEighthSignupReminder(@NonNull Context context, int blockId, char blockLetter, @Nullable String cancelled) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, EIGHTH_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.not_signed_up, blockLetter))
                .setContentText(cancelled != null ? context.getString(R.string.signup_cancelled, cancelled) : null)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(getWebLinkPendingIntent(context, "eighth/signup/" + blockId))
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(blockId, builder.build());
    }

    public static void sendAnnouncementNotification(@NonNull Context context, @NonNull Announcement announcement) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ANNOUNCEMENTS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(announcement.getTitle())
                .setContentText(announcement.getContent())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(getWebLinkPendingIntent(context, "announcements/" + announcement.getId()))
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(announcement.getId(), builder.build());
    }

}
