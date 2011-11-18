package com.daohoangson.chaocovietnam;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class SocketService extends Service {
	SocketServiceBinder binder = new SocketServiceBinder();
	SocketServiceRunnable thread = new SocketServiceRunnable();
	InetAddress broadcastAddress;
	DatagramSocket socketReceiver;
	DatagramSocket socketSender;
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onCreate() {
		 try {
			socketReceiver = new DatagramSocket(Configuration.PORT);
			socketReceiver.setBroadcast(true);
			socketReceiver.setReuseAddress(true);
			//socketReceiver.
			
			socketSender = new DatagramSocket();
			socketSender.setBroadcast(true);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		DhcpInfo dhcp = wifi.getDhcpInfo();
		try {
			int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
			byte[] quads = new byte[4];
			for (int k = 0; k < quads.length; k++) {
				quads[k] = (byte) ((broadcast >> (k*8)) & 0xFF);
				quads[k] = (byte) 255;
			}
			
			broadcastAddress = InetAddress.getByAddress(quads);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			broadcastAddress = null;
		}
		
		// starts the thread
		new Thread(thread).start();
	}
	
	@Override
	public void onDestroy() {
		socketReceiver.close();
		socketSender.close();
		thread.scheduleStop();
		
		super.onDestroy();
	}

	class SocketServiceBinder extends Binder {
		public void broadcast(float seconds) {
			HashMap<String, String> hashmap = new HashMap<String, String>();
			hashmap.put(Configuration.DATA_KEY_SECONDS, "" + seconds);
			hashmap.put(Configuration.DATA_KEY_NAME, "Android");
			JSONObject json = new JSONObject(hashmap);
			String data = json.toString();
			DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), broadcastAddress, Configuration.PORT);
			
			try {
				socketSender.send(packet);
				
				Log.d("CCVN", "Sent " + data);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void setListener(SocketServiceListener listener) {
			thread.listener = listener;
		}
	}
	
	class SocketServiceRunnable implements Runnable {
		SocketServiceListener listener = null;
		protected boolean flagStop = false;
		
		public void scheduleStop() {
			flagStop = true;
		}

		@Override
		public void run() {
			try {
				while (!flagStop) {
					byte[] buf = new byte[256];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socketReceiver.receive(packet);
					
					if (listener != null) {
						String string = new String(packet.getData());
						
						Log.d("CCVN", "Received " + string);
						
						JSONObject jsonObject = new JSONObject(string);
						float seconds = (float) jsonObject.getDouble(Configuration.DATA_KEY_SECONDS);
						String name = jsonObject.getString(Configuration.DATA_KEY_NAME);
						listener.onMessage(seconds, name);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	interface SocketServiceListener {
		public void onMessage(float seconds, String name);
	}
}
