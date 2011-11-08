package com.daohoangson.chaocovietnam;

import java.util.HashMap;
import java.util.Iterator;

import com.daohoangson.chaocovietnam.AudioService.AudioServiceBinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class CCVN extends Activity implements OnClickListener, ServiceConnection {
	Button btnStart = null;
	TextView lblLyrics = null;
	
	private AudioService.AudioServiceBinder audioService = null;
	private Handler handler = new Handler();
	private HashMap<Float, Integer> lyrics = new HashMap<Float, Integer>();
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(this);
        lblLyrics = (TextView) findViewById(R.id.lblLyrics);
        lblLyrics.setText("");
        
        startService(new Intent(this, AudioService.class));
        bindService(new Intent(this, AudioService.class), this, BIND_AUTO_CREATE);
        
        lyrics.put(new Float(8.0f), new Integer(R.string.lyrics_0080));
        lyrics.put(new Float(11.5f), new Integer(R.string.lyrics_0115));
        lyrics.put(new Float(14.5f), new Integer(R.string.lyrics_0145));
        lyrics.put(new Float(20.0f), new Integer(R.string.lyrics_0200));
        lyrics.put(new Float(26.0f), new Integer(R.string.lyrics_0260));
        lyrics.put(new Float(32.0f), new Integer(R.string.lyrics_0320));
        lyrics.put(new Float(37.0f), new Integer(R.string.lyrics_0370));
        lyrics.put(new Float(43.5f), new Integer(R.string.lyrics_0435));
        lyrics.put(new Float(48.5f), new Integer(R.string.lyrics_0485));
        lyrics.put(new Float(52.5f), new Integer(R.string.lyrics_0525));
        lyrics.put(new Float(60.0f), new Integer(R.string.lyrics_0600));
        
        lyrics.put(new Float(67.0f), new Integer(R.string.lyrics_0670));
        lyrics.put(new Float(70.0f), new Integer(R.string.lyrics_0700));
        lyrics.put(new Float(72.5f), new Integer(R.string.lyrics_0725));
        lyrics.put(new Float(77.5f), new Integer(R.string.lyrics_0775));
        lyrics.put(new Float(84.0f), new Integer(R.string.lyrics_0840));
        lyrics.put(new Float(89.0f), new Integer(R.string.lyrics_0890));
        lyrics.put(new Float(94.0f), new Integer(R.string.lyrics_0940));
        lyrics.put(new Float(100.5f), new Integer(R.string.lyrics_1005));
        lyrics.put(new Float(106.5f), new Integer(R.string.lyrics_1065));
        lyrics.put(new Float(109.5f), new Integer(R.string.lyrics_1095));
        lyrics.put(new Float(117.5f), new Integer(R.string.lyrics_1175));
        lyrics.put(new Float(127.5f), new Integer(R.string.lyrics_1275));
    }
    
    public void onDestroy() {
    	unbindService(this);
    	handler.removeCallbacks(audioServiceTick);
    	
    	super.onDestroy();
    }
    
    protected void startPlaying(boolean callService) {
    	if (callService) {
    		audioService.play();
    	}
    	
    	handler.removeCallbacks(audioServiceTick);
		handler.postDelayed(audioServiceTick, 100);
		
    	btnStart.setText(R.string.pause);
    	lblLyrics.setText("");
    }
    
    protected void pausePlaying(boolean callService) {
    	if (callService) {
    		audioService.pause();
    	}
    	
    	handler.removeCallbacks(audioServiceTick);
    	
    	btnStart.setText(R.string.play);
    	lblLyrics.setText("");
    }

	@Override
	public void onClick(View v) {
		if (v == btnStart) {
			if (audioService != null) {
				if (audioService.isPlaying()) {
					pausePlaying(true);
				} else {
					startPlaying(true);
				}
			}
		}
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		audioService = (AudioServiceBinder) service;
		
		// this looks silly but we call it to initialize components' states
		if (audioService.isPlaying()) {
			startPlaying(false);
		} else {
			pausePlaying(false);
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		// TODO Auto-generated method stub
		
	}
	
	private Runnable audioServiceTick = new Runnable() {

		@Override
		public void run() {
			if (audioService != null) {
				float currentTime = audioService.getCurrentPosition() / 1000.0f;
				float maxTime = 0;
				float time = 0;
				int maxLyric = 0;
				
				Iterator<Float> i = lyrics.keySet().iterator();
				while (i.hasNext()) {
					time = i.next();
					if (currentTime > time && maxTime < time) {
						maxTime = time;
						maxLyric = lyrics.get(time);
					}
				}
				
				if (maxLyric > 0) {
					lblLyrics.setText(maxLyric);
				} else {
					lblLyrics.setText("");
				}
				
				if (audioService.isPlaying()) {
					handler.postDelayed(this, 500);
				} else {
					pausePlaying(false);
				}
			}
		}
		
	};
}