package com.daohoangson.chaocovietnam;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

public class AudioService extends Service implements ServiceConnection, SocketService.SocketServiceListener, MediaPlayer.OnCompletionListener {

    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mMediaPlayer = null;
    final private AudioServiceBinder mBinder = new AudioServiceBinder();
    final private Tick mTick = new Tick();

    private SocketService.SocketServiceBinder mSocketService;
    private CCVN mListener;

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        bindService(new Intent(getApplicationContext(), SocketService.class), this, BIND_AUTO_CREATE);

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        unbindService(this);

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        mTick.stop();

        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        mSocketService = (SocketService.SocketServiceBinder) service;
        mSocketService.setListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mSocketService = null;
    }

    @Override
    public void onBroadcastMessage(float seconds, String name) {
        if (mListener != null) {
            mListener.onBroadcastMessage(seconds, name);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mBinder.pause(true);
    }

    class AudioServiceBinder extends Binder {
        public void pause(boolean stop) {
            if (mMediaPlayer != null) {
                if (stop) {
                    mMediaPlayer.seekTo(0);
                }

                mMediaPlayer.pause();

                stopForeground(true);

                mTick.stop();
            }
        }

        public void play() {
            if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.anthem);
                mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                mMediaPlayer.setOnCompletionListener(AudioService.this);
            }

            if (mMediaPlayer != null) {
                mMediaPlayer.start();

                PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                        new Intent(getApplicationContext(), CCVN.class),
                        PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                Notification notification = builder.setContentIntent(pi)
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText(getResources().getString(R.string.notification))
                        .build();
                startForeground(NOTIFICATION_ID, notification);

                mTick.start();
            }
        }

        public void seekRelative(float percentage) {
            if (mMediaPlayer != null) {
                final int current = getCurrentPosition();
                final int duration = getDuration();
                final int delta = (int) (duration * percentage);
                final int seekTo = Math.min(duration, Math.max(0, current + delta));
                mMediaPlayer.seekTo(seekTo);

                if (mListener != null) {
                    mListener.updateLyrics(seekTo / 1000.0f, null);
                }
            }
        }

        public boolean isPlaying() {
            return mMediaPlayer != null && mMediaPlayer.isPlaying();
        }

        public int getCurrentPosition() {
            if (mMediaPlayer != null) {
                return mMediaPlayer.getCurrentPosition();
            } else {
                return 0;
            }
        }

        public int getDuration() {
            if (mMediaPlayer != null) {
                return mMediaPlayer.getDuration();
            } else {
                return 0;
            }
        }

        public void setListener(CCVN listener) {
            mListener = listener;
        }
    }

    class Tick implements Runnable {
        final private Handler mHandler = new Handler();
        private long mBroadcastTime;

        public void start() {
            mBroadcastTime = 0;

            mHandler.removeCallbacks(this);
            mHandler.postDelayed(this, 100);
        }

        public void stop() {
            mHandler.removeCallbacks(this);
        }

        @Override
        public void run() {
            float seconds = mBinder.getCurrentPosition() / 1000.0f;

            if (mListener != null) {
                mListener.updateLyrics(seconds, null);
            }

            if (mBinder.isPlaying()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - mBroadcastTime > Configuration.SYNC_BROADCAST_STEP) {
                    // it's time to broadcast
                    if (mSocketService != null) {
                        mSocketService.broadcast(seconds);
                    }

                    // marks as sent
                    mBroadcastTime = currentTime;
                }

                mHandler.postDelayed(this, Configuration.TIMER_STEP);
            } else {
                if (mListener != null) {
                    mListener.pausePlaying(false, false);
                }
            }
        }

    }
}
