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
	private final static String TAG = "SocketService";

    WifiManager.MulticastLock wifiMulticastLock;

	SocketServiceBinder binder = new SocketServiceBinder();
	ReceiverRunnable receiverRunnable = new ReceiverRunnable();
	SenderRunnable senderRunnable = new SenderRunnable();
	InetAddress broadcastAddress;

	@Override
	public IBinder onBind(Intent intent) {
        wifiMulticastLock.acquire();

		return binder;
	}

    @Override
    public boolean onUnbind(Intent intent) {
        wifiMulticastLock.release();

        return super.onUnbind(intent);
    }

    @Override
	public void onCreate() {
		new Thread(receiverRunnable).start();
		new Thread(senderRunnable).start();

		try {
			WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            wifiMulticastLock = wifi.createMulticastLock(TAG);

			DhcpInfo dhcp = wifi.getDhcpInfo();
			try {
				int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
				byte[] quads = new byte[4];
				for (int k = 0; k < quads.length; k++) {
					quads[k] = (byte) ((broadcast >> (k * 8)) & 0xFF);
					quads[k] = (byte) 255;
				}

				broadcastAddress = InetAddress.getByAddress(quads);
			} catch (UnknownHostException e) {
				e.printStackTrace();

				broadcastAddress = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		if (receiverRunnable != null) {
			receiverRunnable.scheduleStop();
		}

		if (senderRunnable != null) {
			senderRunnable.scheduleStop();
		}

		super.onDestroy();
	}

	class SocketServiceBinder extends Binder {
		public void broadcast(float seconds) {
			HashMap<String, String> hashmap = new HashMap<String, String>();
			hashmap.put(Configuration.DATA_KEY_SECONDS, "" + seconds);
			hashmap.put(Configuration.DATA_KEY_NAME, "Android");
			JSONObject json = new JSONObject(hashmap);
			String data = json.toString();

			if (senderRunnable != null) {
				senderRunnable.packet = new DatagramPacket(data.getBytes(),
						data.length(), broadcastAddress, Configuration.PORT);
			}
		}

		public void setListener(SocketServiceListener listener) {
			receiverRunnable.listener = listener;
		}
	}

	class ReceiverRunnable implements Runnable {
		protected DatagramSocket socketReceiver;
		protected SocketServiceListener listener = null;
		protected boolean flagStop = false;

		public void scheduleStop() {
			flagStop = true;
		}

		@Override
		public void run() {
			boolean connected = false;
			int count = 3;

			while (!connected && count > 0) {
				// we may have to retry a few times
				socketReceiver = null;

				try {
					socketReceiver = new DatagramSocket(Configuration.PORT);
					socketReceiver.setBroadcast(true);
					socketReceiver.setReuseAddress(true);

					connected = true;

					Log.i(TAG, "receiver socket established");
				} catch (SocketException e) {
					e.printStackTrace();

					if (socketReceiver != null) {
						socketReceiver.close();
					}
				}

				count--;
			}

			if (socketReceiver == null || !socketReceiver.isBound()) {
				// something is wrong...
				Log.e(TAG, "receiver socket could not be established");

				return;
			}

			try {
				while (!flagStop) {
					byte[] buf = new byte[256];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socketReceiver.receive(packet);

					if (listener != null) {
						String string = new String(packet.getData());

						Log.v(TAG, "packet received: " + string);

						JSONObject jsonObject = new JSONObject(string);
						float seconds = (float) jsonObject
								.getDouble(Configuration.DATA_KEY_SECONDS);
						String name = jsonObject
								.getString(Configuration.DATA_KEY_NAME);
						listener.onMessage(seconds, name);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}

			try {
				socketReceiver.close();
			} catch (Exception e) {
				// this is expected
			}

            Log.i(TAG, "receiver socket closed");
		}
	}

	interface SocketServiceListener {
		public void onMessage(float seconds, String name);
	}

	class SenderRunnable implements Runnable {
		DatagramSocket socketSender;
		protected DatagramPacket packet = null;
		protected boolean flagStop = false;

		public void scheduleStop() {
			flagStop = true;
		}

		@Override
		public void run() {
			try {
				socketSender = new DatagramSocket();
				socketSender.setBroadcast(true);

				Log.i(TAG, "sender socket established");
			} catch (Exception e) {
				e.printStackTrace();

				if (socketSender != null) {
					socketSender.close();
				}
			}

			if (socketSender == null) {
				// something is wrong...
				Log.e(TAG, "sender socket could not be established");

				return;
			}

			try {
				while (!flagStop) {
					if (packet != null) {
						socketSender.send(packet);
						packet = null;

						Log.v(TAG, "broadcasted a packet");
					}

					Thread.sleep(100);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			try {
				socketSender.close();
			} catch (Exception e) {
				// this is expected
			}

            Log.i(TAG, "sender socket closed");
		}
	}
}
