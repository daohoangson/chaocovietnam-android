package com.daohoangson.chaocovietnam;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class BackupAgent extends BackupAgentHelper {

    final private static String BACKUP_KEY_PREF = "pref";

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferencesBackupHelper helper =
                new SharedPreferencesBackupHelper(this, Configuration.PREF_NAME);
        addHelper(BACKUP_KEY_PREF, helper);
    }
}
