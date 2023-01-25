package me.garrett.ionapp;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

public final class DebugUtils {
    private DebugUtils() {
    }

    public static @NonNull PrintWriter getPrintWriter(@NonNull Context context, @NonNull String filename, boolean append) throws IOException {
        return new PrintWriter(new FileWriter(new File(context.getExternalFilesDir(null), filename), append));
    }

    public static void writeLine(@NonNull Context context, @NonNull String filename, @NonNull String string) {
        try (PrintWriter writer = getPrintWriter(context, filename, true)) {
            writer.println(Instant.now() + " " + string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
