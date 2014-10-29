package com.daohoangson.chaocovietnam;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Configuration {
    final public static int PORT = 25296 + 2;
    final public static int TIMER_STEP = 500; // ms
    final public static int SYNC_BROADCAST_STEP = 2000; // ms
    final public static int SYNC_MAX_DURATION = 5000; // ms
    final public static String DATA_KEY_SECONDS = "s";
    final public static String DATA_KEY_NAME = "n";

    final private static String PREF_SHOW_LYRICS = "showLyrics";
    final private static String PREF_SHOW_PROGRESS = "showProgress";

    public static void setDefaultShowLyrics(Context context, boolean showLyrics) {
        getSharedRef(context).edit().putBoolean(PREF_SHOW_LYRICS, showLyrics).apply();
    }

    public static boolean getDefaultShowLyrics(Context context) {
        return getSharedRef(context).getBoolean(PREF_SHOW_LYRICS, true);
    }

    public static void setDefaultShowProgress(Context context, boolean showProgress) {
        getSharedRef(context).edit().putBoolean(PREF_SHOW_PROGRESS, showProgress).apply();
    }

    public static boolean getDefaultShowProgress(Context context) {
        return getSharedRef(context).getBoolean(PREF_SHOW_PROGRESS, true);
    }

    private static SharedPreferences getSharedRef(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
