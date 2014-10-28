package com.daohoangson.chaocovietnam.adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.view.Display;

import com.daohoangson.chaocovietnam.R;

public class Config implements Parcelable {

    private static int maxDisplayWidth = 0;
    private static int maxDisplayHeight = 0;

    private boolean hasDisplay;
    private int displayId;
    private String displayName;
    private int displayWidth;
    private int displayHeight;

    private boolean showLyrics;
    private boolean showProgress;

    private OnConfigChangeListener mListener;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(displayId);
        parcel.writeInt(showLyrics ? 1 : 0);
        parcel.writeInt(showProgress ? 1 : 0);
    }

    public static final Parcelable.Creator<Config> CREATOR
            = new Parcelable.Creator<Config>() {
        public Config createFromParcel(Parcel in) {
            return new Config(in);
        }

        public Config[] newArray(int size) {
            return new Config[size];
        }
    };

    public Config() {
        hasDisplay = false;
    }

    private Config(Parcel in) {
        displayId = in.readInt();
        showLyrics = in.readInt() == 1;
        showProgress = in.readInt() == 1;
        hasDisplay = false;
    }

    public void setListener(OnConfigChangeListener listener) {
        mListener = listener;

        if (mListener != null) {
            mListener.onConfigChange(this);
        }
    }

    public void notifyListener() {
        if (mListener != null) {
            mListener.onConfigChange(this);
        }
    }

    public void populateDisplay(Context context, Display display) {
        displayId = display.getDisplayId();
        populateDisplayName(context, display);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        displayWidth = displayMetrics.widthPixels;
        displayHeight = displayMetrics.heightPixels;

        maxDisplayWidth = Math.max(maxDisplayWidth, displayWidth);
        maxDisplayHeight = Math.max(maxDisplayHeight, displayHeight);

        hasDisplay = true;
    }

    public void resetDisplayMax() {
        maxDisplayWidth = 0;
        maxDisplayHeight = 0;
    }

    public void populateDefaults() {
        showLyrics = true;
        showProgress = false;
    }

    public String getDisplayName() {
        checkHasDisplay();

        return displayName;
    }

    public int getDisplayWidth() {
        checkHasDisplay();

        return displayWidth;
    }

    public int getDisplayHeight() {
        checkHasDisplay();

        return displayHeight;
    }

    public int getMaxDisplayWidth() {
        checkHasDisplay();

        return maxDisplayWidth;
    }

    public int getMaxDisplayHeight() {
        checkHasDisplay();

        return maxDisplayHeight;
    }

    public void setShowLyrics(boolean b) {
        showLyrics = b;
    }

    public boolean getShowLyrics() {
        checkHasDisplay();

        return showLyrics;
    }

    public void setShowProgress(boolean b) {
        showProgress = b;
    }

    public boolean getShowProgress() {
        checkHasDisplay();

        return showProgress;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void populateDisplayName(Context context, Display display) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            displayName = display.getName();
        } else {
            displayName = context.getString(R.string.display_x, displayId);
        }
    }

    private void checkHasDisplay() {
        if (!hasDisplay) {
            throw new IllegalAccessError("Config is incomplete (no display information).");
        }
    }

    public interface OnConfigChangeListener {
        public void onConfigChange(Config config);
    }

}