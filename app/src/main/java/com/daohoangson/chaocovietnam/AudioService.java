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

    MediaPlayer mediaPlayer = null;
    AudioServiceBinder binder = new AudioServiceBinder();
    Tick tick = new Tick();

    SocketService.SocketServiceBinder socketService;
    CCVN listener;

    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    @Override
    public void onCreate() {
        bindService(new Intent(getApplicationContext(), SocketService.class), this, BIND_AUTO_CREATE);

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        unbindService(this);

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        tick.stop();

        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        socketService = (SocketService.SocketServiceBinder) service;
        socketService.setListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        socketService = null;
    }

    @Override
    public void onMessage(float seconds, String name) {
        if (listener != null) {
            listener.onMessage(seconds, name);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        binder.pause();
    }

    class AudioServiceBinder extends Binder {
        public void pause() {
            if (mediaPlayer != null) {
                mediaPlayer.pause();

                stopForeground(true);

                tick.stop();
            }
        }

        public void play() {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.anthem);
                mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                mediaPlayer.setOnCompletionListener(AudioService.this);
            }

            if (mediaPlayer != null) {
                mediaPlayer.start();

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

                tick.start();
            }
        }

        public boolean isPlaying() {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        }

        public int getCurrentPosition() {
            if (mediaPlayer != null) {
                return mediaPlayer.getCurrentPosition();
            } else {
                return 0;
            }
        }

        public void setListener(CCVN listener) {
            AudioService.this.listener = listener;
        }
    }

    class Tick implements Runnable {
        protected Handler audioServiceHandler = new Handler();
        protected long broadcastSentTime;

        public void start() {
            broadcastSentTime = 0;

            audioServiceHandler.removeCallbacks(this);
            audioServiceHandler.postDelayed(this, 100);
        }

        public void stop() {
            audioServiceHandler.removeCallbacks(this);
        }

        @Override
        public void run() {
            float seconds = binder.getCurrentPosition() / 1000.0f;

            if (listener != null) {
                listener.updateLyrics(seconds, null);
            }

            if (binder.isPlaying()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - broadcastSentTime > Configuration.SYNC_BROADCAST_STEP) {
                    // it's time to broadcast
                    if (socketService != null) {
                        socketService.broadcast(seconds);
                    }

                    // marks as sent
                    broadcastSentTime = currentTime;
                }

                audioServiceHandler.postDelayed(this, Configuration.TIMER_STEP);
            } else {
                if (listener != null) {
                    listener.pausePlaying(false);
                }
            }
        }

    }
}
