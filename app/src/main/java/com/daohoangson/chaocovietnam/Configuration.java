package com.daohoangson.chaocovietnam;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;

public class Configuration {
    final public static int PORT = 25296 + 2;
    final public static int TIMER_STEP = 500; // ms
    final public static int SYNC_BROADCAST_STEP = 2000; // ms
    final public static int SYNC_MAX_DURATION = 5000; // ms
    final public static String DATA_KEY_SECONDS = "s";
    final public static String DATA_KEY_NAME = "n";

    final public static String PREF_NAME = "pref";
    final private static String PREF_SHOW_LYRICS = "showLyrics";
    final private static String PREF_SHOW_PROGRESS = "showProgress";

    public static void setDefaultShowLyrics(Context context, boolean showLyrics) {
        getSharedRef(context).edit().putBoolean(PREF_SHOW_LYRICS, showLyrics).apply();
        requestBackup(context);
    }

    public static boolean getDefaultShowLyrics(Context context) {
        return getSharedRef(context).getBoolean(PREF_SHOW_LYRICS, true);
    }

    public static void setDefaultShowProgress(Context context, boolean showProgress) {
        getSharedRef(context).edit().putBoolean(PREF_SHOW_PROGRESS, showProgress).apply();
        requestBackup(context);
    }

    public static boolean getDefaultShowProgress(Context context) {
        return getSharedRef(context).getBoolean(PREF_SHOW_PROGRESS, true);
    }

    private static SharedPreferences getSharedRef(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static void requestBackup(Context context) {
        if (!BuildConfig.DEBUG) {
            // only notify backup manager of data changed if we are running in release
            // the backup key is valid for the release package only anyway
            new BackupManager(context).dataChanged();
        }
    }
}
