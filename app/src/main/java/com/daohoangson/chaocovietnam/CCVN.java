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
import android.view.View;
import android.widget.TextView;

import com.daohoangson.chaocovietnam.AudioService.AudioServiceBinder;

import java.util.HashMap;

public class CCVN extends Activity implements StarView.OnStarInteraction,
        ServiceConnection, SocketService.SocketServiceListener {

    private final static String TAG = "CCVN/Main";

    StarView starView = null;
    TextView lblLyrics = null;

    protected AudioService.AudioServiceBinder audioService = null;

    protected Handler socketServiceHandler = new Handler();
    protected long syncBaseTime = 0;
    protected String syncDeviceName = null;
    protected long syncUpdatedTime = 0;

    protected HashMap<Float, Integer> lyrics = new HashMap<Float, Integer>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        starView = (StarView) findViewById(R.id.star);
        starView.setListener(this);

        lblLyrics = (TextView) findViewById(R.id.lblLyrics);
        lblLyrics.setText("");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        lyrics.put(8.0f, R.string.lyrics_0080);
        lyrics.put(11.5f, R.string.lyrics_0115);
        lyrics.put(14.5f, R.string.lyrics_0145);
        lyrics.put(20.0f, R.string.lyrics_0200);
        lyrics.put(26.0f, R.string.lyrics_0260);
        lyrics.put(32.0f, R.string.lyrics_0320);
        lyrics.put(37.0f, R.string.lyrics_0370);
        lyrics.put(43.5f, R.string.lyrics_0435);
        lyrics.put(48.5f, R.string.lyrics_0485);
        lyrics.put(52.5f, R.string.lyrics_0525);
        lyrics.put(60.0f, R.string.lyrics_0600);

        lyrics.put(67.0f, R.string.lyrics_0670);
        lyrics.put(70.0f, R.string.lyrics_0700);
        lyrics.put(72.5f, R.string.lyrics_0725);
        lyrics.put(77.5f, R.string.lyrics_0775);
        lyrics.put(84.0f, R.string.lyrics_0840);
        lyrics.put(89.0f, R.string.lyrics_0890);
        lyrics.put(94.0f, R.string.lyrics_0940);
        lyrics.put(100.5f, R.string.lyrics_1005);
        lyrics.put(106.5f, R.string.lyrics_1065);
        lyrics.put(109.5f, R.string.lyrics_1095);
        lyrics.put(117.5f, R.string.lyrics_1175);
        lyrics.put(127.5f, R.string.lyrics_1275);
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

        if (audioService != null) {
            if (!audioService.isPlaying()) {
                stopService(new Intent(this, AudioService.class));
            }

            unbindService(this);
        }
    }

    @Override
    public void onDestroy() {
        socketServiceHandler.removeCallbacks(socketServiceTick);

        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            starView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onStarClick() {
        if (audioService != null && audioService.isPlaying()) {
            pausePlaying(true);
        } else {
            startPlaying(true);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        audioService = (AudioServiceBinder) service;
        audioService.setListener(this);

        // this looks silly but we call it to initialize components' states
        if (audioService.isPlaying()) {
            startPlaying(false);
        } else {
            pausePlaying(false);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        audioService = null;
    }

    public void startPlaying(boolean callService) {
        if (callService && audioService != null) {
            audioService.play();
        }

        lblLyrics.setText("");

        syncBaseTime = 0;
        syncDeviceName = null;
        syncUpdatedTime = 0;
    }

    public void pausePlaying(boolean callService) {
        if (callService) {
            audioService.pause();
        }

        lblLyrics.setText("");
    }

    public void updateLyrics(float seconds, String fromDeviceName) {
        float maxTime = 0;
        int maxLyric = 0;

        for (Float time : lyrics.keySet()) {
            if (seconds > time && maxTime < time) {
                maxTime = time;
                maxLyric = lyrics.get(time);
            }
        }

        if (maxLyric > 0) {
            if (fromDeviceName == null) {
                lblLyrics.setText(maxLyric);
            } else {
                // this is from another device (sync mode)
                // appends the device name
                String line = getResources().getString(maxLyric);
                String formatted = String.format("%s (%s)", line,
                        fromDeviceName);
                lblLyrics.setText(formatted);
            }
        } else {
            lblLyrics.setText("");
        }
    }

    protected Runnable socketServiceTick = new Runnable() {

        @Override
        public void run() {
            if (syncBaseTime == 0
                    || (audioService != null && audioService.isPlaying())) {
                // nothing to do here
                return;
            }

            long currentTime = System.currentTimeMillis();
            long baseOffset = currentTime - syncBaseTime;
            long updatedOffset = currentTime - syncUpdatedTime;

            if (updatedOffset > Configuration.SYNC_MAX_DURATION) {
                // no signal from the host for too long
                // reset control state and stop looping
                pausePlaying(false);
                return;
            }

            // updates the lyrics using the normal flow code
            updateLyrics(baseOffset / 1000.0f, syncDeviceName);

            // schedule this function again...
            socketServiceHandler.postDelayed(this, Configuration.TIMER_STEP);
        }

    };

    @Override
    public void onMessage(final float seconds, final String name) {
        socketServiceHandler.post(new Runnable() {

            @Override
            public void run() {
                if (audioService == null || !audioService.isPlaying()) {
                    // only works if the player is not playing
                    // this check will keeps us from working with our own
                    // broadcast message
                    // that's silly!
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - syncUpdatedTime > 1000) {
                        // checks to deal with double udp message (ipv4 and
                        // ipv6)
                        syncBaseTime = currentTime - ((long) (seconds * 1000));
                        syncDeviceName = name;
                        syncUpdatedTime = currentTime;

                        socketServiceHandler.removeCallbacks(socketServiceTick);
                        socketServiceHandler.post(socketServiceTick);
                    }
                }
            }

        });
    }

}