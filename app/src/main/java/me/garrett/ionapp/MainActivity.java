package me.garrett.ionapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import me.garrett.ionapp.api.CheckScheduleWorker;
import me.garrett.ionapp.api.IonApi;
import me.garrett.ionapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    static final String CHECK_SCHEDULE_UNIQUE_WORK_NAME = "CheckIonSchedule";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        NotificationChannel busChannel = new NotificationChannel(
                Notifications.BUS_CHANNEL_ID, getString(R.string.bus_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(busChannel);

        NotificationChannel testChannel = new NotificationChannel(
                Notifications.TEST_CHANNEL_ID, "Tests",
                NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(testChannel);

        NotificationChannel eighthChannel = new NotificationChannel(
                Notifications.EIGHTH_CHANNEL_ID, getString(R.string.eighth_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(eighthChannel);

        NotificationChannel announcementsChannel = new NotificationChannel(
                Notifications.ANNOUNCEMENTS_CHANNEL_ID, getString(R.string.announcements_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(announcementsChannel);

        PeriodicWorkRequest checkScheduleRequest = new
                PeriodicWorkRequest.Builder(CheckScheduleWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                CHECK_SCHEDULE_UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                checkScheduleRequest);

        // redirect to login activity if not authorized
        if (!IonApi.getInstance(this).isAuthorized()) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        // Explicit permission request for notifications required on API level 33 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);

            }

        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_announcements, R.id.navigation_schedule, R.id.navigation_eighth, R.id.navigation_bus)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.popup_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            IonApi.getInstance(this).clearAuthState();
            startActivity(new Intent(this, LoginActivity.class));
            return true;
        } else if (id == R.id.action_test_activity) {
            startActivity(new Intent(this, TestActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}