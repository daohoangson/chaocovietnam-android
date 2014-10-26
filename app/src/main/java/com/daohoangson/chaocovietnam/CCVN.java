package com.daohoangson.chaocovietnam;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.SparseArray;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;

import com.daohoangson.chaocovietnam.AudioService.AudioServiceBinder;
import com.daohoangson.chaocovietnam.fragment.FlagFragment;

import java.util.HashMap;

public class CCVN extends FragmentActivity implements
        FlagFragment.Caller,
        ServiceConnection,
        SocketService.SocketServiceListener {

    private AudioService.AudioServiceBinder mAudioService = null;

    private FrameLayout mContainer;
    private FlagFragment mFlagFragment;

    final private Handler mHandler = new Handler();
    private long mSyncBaseTime = 0;
    private String mSyncDeviceName = null;
    private long mSyncUpdatedTime = 0;

    private final SparseArray<Dialog> mPresentations = new SparseArray<Dialog>();
    private DisplayManager mDisplayManager;
    private Object mDisplayListener;

    final private HashMap<Float, Integer> mLyrics = new HashMap<Float, Integer>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        mContainer = (FrameLayout) findViewById(R.id.container);

        mLyrics.put(8.0f, R.string.lyrics_0080);
        mLyrics.put(11.5f, R.string.lyrics_0115);
        mLyrics.put(14.5f, R.string.lyrics_0145);
        mLyrics.put(20.0f, R.string.lyrics_0200);
        mLyrics.put(26.0f, R.string.lyrics_0260);
        mLyrics.put(32.0f, R.string.lyrics_0320);
        mLyrics.put(37.0f, R.string.lyrics_0370);
        mLyrics.put(43.5f, R.string.lyrics_0435);
        mLyrics.put(48.5f, R.string.lyrics_0485);
        mLyrics.put(52.5f, R.string.lyrics_0525);
        mLyrics.put(60.0f, R.string.lyrics_0600);

        mLyrics.put(67.0f, R.string.lyrics_0670);
        mLyrics.put(70.0f, R.string.lyrics_0700);
        mLyrics.put(72.5f, R.string.lyrics_0725);
        mLyrics.put(77.5f, R.string.lyrics_0775);
        mLyrics.put(84.0f, R.string.lyrics_0840);
        mLyrics.put(89.0f, R.string.lyrics_0890);
        mLyrics.put(94.0f, R.string.lyrics_0940);
        mLyrics.put(100.5f, R.string.lyrics_1005);
        mLyrics.put(106.5f, R.string.lyrics_1065);
        mLyrics.put(109.5f, R.string.lyrics_1095);
        mLyrics.put(117.5f, R.string.lyrics_1175);
        mLyrics.put(127.5f, R.string.lyrics_1275);

        flagOnCreate(savedInstanceState);
        presentationOnCreate();
    }

    @Override
    protected void onResume() {
        super.onResume();

        startService(new Intent(this, AudioService.class));
        bindService(new Intent(this, AudioService.class), this, BIND_AUTO_CREATE);

        presentationOnResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mAudioService != null) {
            if (!mAudioService.isPlaying()) {
                stopService(new Intent(this, AudioService.class));
            }

            unbindService(this);
        }

        presentationOnPause();
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(socketServiceTick);

        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mContainer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mAudioService = (AudioServiceBinder) service;
        mAudioService.setListener(this);

        // this looks silly but we call it to initialize components' states
        if (mAudioService.isPlaying()) {
            startPlaying(false);
        } else {
            pausePlaying(false, false);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mAudioService = null;
    }

    @Override
    public void onBroadcastMessage(final float seconds, final String name) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                if (mAudioService == null || !mAudioService.isPlaying()) {
                    // only works if the player is not playing
                    // this check will keeps us from working with our own
                    // broadcast message, that's silly!
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - mSyncUpdatedTime > 1000) {
                        // checks to deal with double udp message (ipv4 and
                        // ipv6)
                        mSyncBaseTime = currentTime - ((long) (seconds * 1000));
                        mSyncDeviceName = name;
                        mSyncUpdatedTime = currentTime;

                        mHandler.removeCallbacks(socketServiceTick);
                        mHandler.post(socketServiceTick);
                    }
                }
            }

        });
    }

    @Override
    public void setFlagFragment(FlagFragment flagFragment) {
        mFlagFragment = flagFragment;
    }

    @Override
    public void startPlaying(boolean callService) {
        if (callService && mAudioService != null) {
            mAudioService.play();
        }

        mSyncBaseTime = 0;
        mSyncDeviceName = null;
        mSyncUpdatedTime = 0;
    }

    @Override
    public void pausePlaying(boolean callService, boolean stop) {
        if (callService) {
            mAudioService.pause(stop);
        }
    }

    @Override
    public boolean isPlaying() {
        return mAudioService != null && mAudioService.isPlaying();
    }

    @Override
    public void seekRelative(float percentage) {
        if (mAudioService != null) {
            mAudioService.seekRelative(percentage);
        }
    }

    public void updateLyrics(float seconds, String fromDeviceName) {
        float maxTime = 0;
        int maxLyric = 0;
        final float progress = (seconds == 0 ? 0 :
                (mAudioService.getCurrentPosition() * 1.0f / mAudioService.getDuration()));

        for (Float time : mLyrics.keySet()) {
            if (seconds > time && maxTime < time) {
                maxTime = time;
                maxLyric = mLyrics.get(time);
            }
        }

        final String lyric;
        if (maxLyric > 0) {
            if (fromDeviceName == null) {
                lyric = getResources().getString(maxLyric);
            } else {
                // this is from another device (sync mode)
                // appends the device name
                String line = getResources().getString(maxLyric);
                lyric = String.format("%s (%s)", line, fromDeviceName);
            }
        } else {
            lyric = "";
        }

        flagUpdateLyrics(lyric, progress);
        presentationUpdateLyrics(lyric, progress);
    }

    private void flagUpdateLyrics(String lyric, float progress) {
        if (mFlagFragment == null) {
            return;
        }

        mFlagFragment.getLyricsView().setText(lyric);
        mFlagFragment.getStarView().setProgress(progress);
    }

    private void flagOnCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new FlagFragment())
                    .commit();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void showPresentation(Display display) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return;
        }

        final int displayId = display.getDisplayId();
        if (mPresentations.get(displayId) != null) {
            return;
        }

        Dialog presentation = new CcvnPresentation(this, display);
        presentation.show();
        presentation.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mPresentations.delete(displayId);
            }
        });

        mPresentations.put(displayId, presentation);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void presentationUpdateLyrics(String lyric, float progress) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return;
        }

        for (int i = 0; i < mPresentations.size(); i++) {
            CcvnPresentation presentation = (CcvnPresentation) mPresentations.valueAt(i);
            presentation.getLyricsView().setText(lyric);
            presentation.getStarView().setProgress(progress);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void presentationOnCreate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return;
        }

        mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
                showPresentation(mDisplayManager.getDisplay(displayId));
            }

            @Override
            public void onDisplayRemoved(int displayId) {
                // do nothing
            }

            @Override
            public void onDisplayChanged(int displayId) {
                // do nothing
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void presentationOnResume() {
        if (mDisplayManager == null || mDisplayListener == null) {
            return;
        }

        Display[] displays = mDisplayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        for (Display display : displays) {
            showPresentation(display);
        }

        mDisplayManager.registerDisplayListener((DisplayManager.DisplayListener) mDisplayListener, null);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void presentationOnPause() {
        if (mDisplayManager == null || mDisplayListener == null) {
            return;
        }

        mDisplayManager.unregisterDisplayListener((DisplayManager.DisplayListener) mDisplayListener);

        for (int i = 0; i < mPresentations.size(); i++) {
            Dialog presentation = mPresentations.valueAt(i);
            presentation.dismiss();
        }
        mPresentations.clear();
    }

    final private Runnable socketServiceTick = new Runnable() {

        @Override
        public void run() {
            if (mSyncBaseTime == 0
                    || (mAudioService != null && mAudioService.isPlaying())) {
                // nothing to do here
                return;
            }

            long currentTime = System.currentTimeMillis();
            long baseOffset = currentTime - mSyncBaseTime;
            long updatedOffset = currentTime - mSyncUpdatedTime;

            if (updatedOffset > Configuration.SYNC_MAX_DURATION) {
                // no signal from the host for too long
                // reset control state and stop looping
                pausePlaying(false, false);
                return;
            }

            // updates the lyrics using the normal flow code
            updateLyrics(baseOffset / 1000.0f, mSyncDeviceName);

            // schedule this function again...
            mHandler.postDelayed(this, Configuration.TIMER_STEP);
        }

    };
}