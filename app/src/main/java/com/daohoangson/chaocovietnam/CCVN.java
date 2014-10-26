package com.daohoangson.chaocovietnam;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.daohoangson.chaocovietnam.AudioService.AudioServiceBinder;

import java.util.HashMap;

public class CCVN extends Activity implements ServiceConnection,
        SocketService.SocketServiceListener,
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private StarView mStarView;
    private TextView mLyricsView;
    private GestureDetectorCompat mGDC;

    private AudioService.AudioServiceBinder mAudioService = null;

    final private Handler mHandler = new Handler();
    private long mSyncBaseTime = 0;
    private String mSyncDeviceName = null;
    private long mSyncUpdatedTime = 0;

    final private HashMap<Float, Integer> mLyrics = new HashMap<Float, Integer>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mStarView = (StarView) findViewById(R.id.star);
        mLyricsView = (TextView) findViewById(R.id.lblLyrics);
        mLyricsView.setText("");

        mGDC = new GestureDetectorCompat(this, this);
        mGDC.setOnDoubleTapListener(this);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

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
    }

    @Override
    protected void onResume() {
        super.onResume();

        startService(new Intent(this, AudioService.class));
        bindService(new Intent(this, AudioService.class), this, BIND_AUTO_CREATE);
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
            mStarView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGDC.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        if (mAudioService != null && mAudioService.isPlaying()) {
            pausePlaying(true, false);
        } else {
            startPlaying(true);
        }

        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        if (mAudioService == null) {
            startPlaying(true);
        } else {
            pausePlaying(true, true);
            startPlaying(true);
        }

        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return false;
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

    public void startPlaying(boolean callService) {
        if (callService && mAudioService != null) {
            mAudioService.play();
        }

        mLyricsView.setText("");

        mSyncBaseTime = 0;
        mSyncDeviceName = null;
        mSyncUpdatedTime = 0;
    }

    public void pausePlaying(boolean callService, boolean stop) {
        if (callService) {
            mAudioService.pause(stop);
        }

        mLyricsView.setText("");
    }

    public void updateLyrics(float seconds, String fromDeviceName) {
        float maxTime = 0;
        int maxLyric = 0;

        for (Float time : mLyrics.keySet()) {
            if (seconds > time && maxTime < time) {
                maxTime = time;
                maxLyric = mLyrics.get(time);
            }
        }

        if (maxLyric > 0) {
            if (fromDeviceName == null) {
                mLyricsView.setText(maxLyric);
            } else {
                // this is from another device (sync mode)
                // appends the device name
                String line = getResources().getString(maxLyric);
                String formatted = String.format("%s (%s)", line,
                        fromDeviceName);
                mLyricsView.setText(formatted);
            }
        } else {
            mLyricsView.setText("");
        }
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