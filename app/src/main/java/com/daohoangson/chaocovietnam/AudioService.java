package com.daohoangson.chaocovietnam;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.media.MediaPlayer;

public class AudioService extends Service {
	MediaPlayer mediaPlayer = null;
	AudioServiceBinder binder = new AudioServiceBinder();

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}

    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
		
		super.onDestroy();
	}
	
	class AudioServiceBinder extends Binder {
		public void pause() {
			if (mediaPlayer != null) {
				mediaPlayer.pause();
			}
		}
		
		public void play() {
			if (mediaPlayer == null) {
				mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.anthem);
			}
			
			if (mediaPlayer != null) {
				mediaPlayer.start();
			}
		}
		
		public boolean isPlaying() {
			if (mediaPlayer != null) {
				return mediaPlayer.isPlaying();
			} else {
				return false;
			}
		}
		
		public int getCurrentPosition() {
			if (mediaPlayer != null) {
				return mediaPlayer.getCurrentPosition();
			} else {
				return 0;
			}
		}
		
		public int getDuration() {
			if (mediaPlayer != null) {
				return mediaPlayer.getDuration();
			} else {
				return 0;
			}
		}
	}
}
