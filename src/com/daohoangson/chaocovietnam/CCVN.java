package com.daohoangson.chaocovietnam;

import java.util.HashMap;
import java.util.Iterator;

import com.daohoangson.chaocovietnam.AudioService.AudioServiceBinder;
import com.daohoangson.chaocovietnam.SocketService.SocketServiceBinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class CCVN extends Activity implements OnClickListener, ServiceConnection, SocketService.SocketServiceListener {
	Button btnStart = null;
	TextView lblLyrics = null;
	
	protected PowerManager.WakeLock wakeLock;
	protected WifiManager.MulticastLock wifiMulticastLock;
	
	protected AudioService.AudioServiceBinder audioService = null;
	protected Handler audioServiceHandler = new Handler();
	
	protected SocketService.SocketServiceBinder socketService = null;
	protected Handler socketServiceHandler = new Handler();
	protected long broadcastSentTime = 0;
	protected long syncBaseTime = 0;
	protected String syncDeviceName = null;
	protected long syncUpdatedTime = 0;
	
	protected HashMap<Float, Integer> lyrics = new HashMap<Float, Integer>();
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(this);
        lblLyrics = (TextView) findViewById(R.id.lblLyrics);
        lblLyrics.setText("");
        
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, Configuration.TAG);
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiMulticastLock = wifiManager.createMulticastLock(Configuration.TAG);
        
        startService(new Intent(this, AudioService.class));
        bindService(new Intent(this, AudioService.class), this, BIND_AUTO_CREATE);
        bindService(new Intent(this, SocketService.class), this, BIND_AUTO_CREATE);
        
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
    
    protected void onResume() {
    	super.onResume();
    	
    	wakeLock.acquire();
    	wifiMulticastLock.acquire();
    }
    
    protected void onPause() {
    	super.onPause();
    	
    	wakeLock.release();
    	wifiMulticastLock.release();
    }
    
    public void onDestroy() {
    	unbindService(this);
    	
    	audioServiceHandler.removeCallbacks(audioServiceTick);
    	socketServiceHandler.removeCallbacks(socketServiceTick);
    	
    	super.onDestroy();
    }
    
    protected void startPlaying(boolean callService) {
    	if (callService) {
    		audioService.play();
    	}
    	
    	audioServiceHandler.removeCallbacks(audioServiceTick);
		audioServiceHandler.postDelayed(audioServiceTick, 100);
		
    	btnStart.setText(R.string.pause);
    	lblLyrics.setText("");
    	
    	broadcastSentTime = 0;
    	syncBaseTime = 0;
    	syncDeviceName = null;
    	syncUpdatedTime = 0;
    }
    
    protected void pausePlaying(boolean callService) {
    	if (callService) {
    		audioService.pause();
    	}
    	
    	audioServiceHandler.removeCallbacks(audioServiceTick);
    	
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
		if (service instanceof AudioServiceBinder) {
			audioService = (AudioServiceBinder) service;
			
			// this looks silly but we call it to initialize components' states
			if (audioService.isPlaying()) {
				startPlaying(false);
			} else {
				pausePlaying(false);
			}
		} else if (service instanceof SocketServiceBinder) {
			socketService = (SocketServiceBinder) service;
			socketService.setListener(this);
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		// TODO Auto-generated method stub
		
	}
	
	protected Runnable audioServiceTick = new Runnable() {

		@Override
		public void run() {
			if (audioService != null) {
				float seconds = audioService.getCurrentPosition() / 1000.0f;
				
				updateLyrics(seconds, null);
				
				if (audioService.isPlaying()) {
					if (socketService != null) {
						long currentTime = System.currentTimeMillis();
						if (currentTime - broadcastSentTime > Configuration.SYNC_BROADCAST_STEP) {
							// it's time to broadcast
							socketService.broadcast(seconds);
							// marks as sent
							broadcastSentTime = currentTime;
						}
					}
					
					audioServiceHandler.postDelayed(this, Configuration.TIMER_STEP);
				} else {
					pausePlaying(false);
				}
			}
		}
		
	};
	
	protected Runnable socketServiceTick = new Runnable() {

		@Override
		public void run() {
			if (syncBaseTime == 0 || (audioService != null && audioService.isPlaying())) {
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
	
	protected void updateLyrics(float seconds, String fromDeviceName) {
		float maxTime = 0;
		float time = 0;
		int maxLyric = 0;
		
		Iterator<Float> i = lyrics.keySet().iterator();
		while (i.hasNext()) {
			time = i.next();
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
				String formatted = String.format("%s (%s)", line, fromDeviceName);
				lblLyrics.setText(formatted);
			}
		} else {
			lblLyrics.setText("");
		}
	}

	@Override
	public void onMessage(final float seconds, final String name) {
		socketServiceHandler.post(new Runnable() {

			@Override
			public void run() {
				if (audioService == null || audioService.isPlaying() == false) {
					// only works if the player is not playing
	                // this check will keeps us from working with our own broadcast message
	                // that's silly!
					long currentTime = System.currentTimeMillis();
					if (currentTime - syncUpdatedTime > 1000) {
						// checks to deal with double udp message (ipv4 and ipv6)
						syncBaseTime = currentTime - ((long) (seconds * 1000));
						syncDeviceName = name;
						syncUpdatedTime = currentTime;
						
						socketServiceHandler.post(socketServiceTick);
					}
				}
			}
			
		});
	}
}