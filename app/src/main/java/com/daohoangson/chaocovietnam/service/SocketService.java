package com.daohoangson.chaocovietnam.service;

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

import com.daohoangson.chaocovietnam.BuildConfig;
import com.daohoangson.chaocovietnam.Configuration;

public class SocketService extends Service {
    private final static String TAG = "SocketService";

    private WifiManager.MulticastLock mMulticastLock;

    final private SocketServiceBinder mBinder = new SocketServiceBinder();
    final private ReceiverRunnable mReceiver = new ReceiverRunnable();
    final private SenderRunnable mSender = new SenderRunnable();
    private InetAddress mBroadcastAddress;

    @Override
    public IBinder onBind(Intent intent) {
        mMulticastLock.acquire();

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mMulticastLock.release();

        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        new Thread(mReceiver).start();
        new Thread(mSender).start();

        try {
            WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            mMulticastLock = wifi.createMulticastLock(TAG);

            DhcpInfo dhcp = wifi.getDhcpInfo();
            try {
                int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
                byte[] quads = new byte[4];
                for (int k = 0; k < quads.length; k++) {
                    quads[k] = (byte) ((broadcast >> (k * 8)) & 0xFF);
                    quads[k] = (byte) 255;
                }

                mBroadcastAddress = InetAddress.getByAddress(quads);
            } catch (UnknownHostException e) {
                e.printStackTrace();

                mBroadcastAddress = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        mReceiver.scheduleStop();
        mSender.scheduleStop();

        super.onDestroy();
    }

    public interface SocketServiceListener {
        public void onBroadcastMessage(float seconds, String name);
    }

    public class SocketServiceBinder extends Binder {
        public void broadcast(float seconds) {
            HashMap<String, String> hashmap = new HashMap<String, String>();
            hashmap.put(Configuration.DATA_KEY_SECONDS, "" + seconds);
            hashmap.put(Configuration.DATA_KEY_NAME, "Android");
            JSONObject json = new JSONObject(hashmap);
            String data = json.toString();

            mSender.mPacket = new DatagramPacket(data.getBytes(),
                    data.length(), mBroadcastAddress, Configuration.PORT);
        }

        public void setListener(SocketServiceListener listener) {
            mReceiver.mListener = listener;
        }
    }

    private class SenderRunnable implements Runnable {
        private DatagramSocket mSocketSender;
        private DatagramPacket mPacket = null;
        private boolean mFlagStop = false;

        public void scheduleStop() {
            mFlagStop = true;
        }

        @Override
        public void run() {
            try {
                mSocketSender = new DatagramSocket();
                mSocketSender.setBroadcast(true);

                Log.i(TAG, "sender socket established");
            } catch (Exception e) {
                Log.e(TAG, "sender socket opening", e);

                if (mSocketSender != null) {
                    try {
                        mSocketSender.close();
                    } catch (Exception e2) {
                        // ignore
                    }
                }
            }

            if (mSocketSender == null) {
                // something is wrong...
                Log.e(TAG, "sender socket could not be established");
                return;
            }

            try {
                while (!mFlagStop) {
                    if (mPacket != null) {
                        mSocketSender.send(mPacket);
                        mPacket = null;

                        Log.v(TAG, "broadcast a packet");
                    }

                    Thread.sleep(100);
                }
            } catch (Exception e) {
                Log.e(TAG, "sender socket", e);
            }

            try {
                mSocketSender.close();
            } catch (Exception e) {
                Log.e(TAG, "sender socket closing", e);
            }

            Log.i(TAG, "sender socket closed");
        }
    }

    private class ReceiverRunnable implements Runnable {
        private DatagramSocket mSocketReceiver;
        private SocketServiceListener mListener = null;

        private void scheduleStop() {
            try {
                mSocketReceiver.close();
            } catch (Exception e) {
                Log.e(TAG, "receiver socket closing", e);
            }
        }

        @Override
        public void run() {
            boolean connected = false;
            int count = 3;

            while (!connected && count > 0) {
                // we may have to retry a few times
                mSocketReceiver = null;

                try {
                    mSocketReceiver = new DatagramSocket(Configuration.PORT);
                    mSocketReceiver.setBroadcast(true);
                    mSocketReceiver.setReuseAddress(true);

                    connected = true;

                    Log.i(TAG, "receiver socket established");
                } catch (SocketException e) {
                    Log.e(TAG, "receiver socket opening");

                    if (mSocketReceiver != null) {
                        try {
                            mSocketReceiver.close();
                        } catch (Exception e2) {
                            // ignore
                        }
                    }

                    try {
                        // wait a bit before trying again
                        Thread.sleep(500);
                    } catch (InterruptedException e2) {
                        // ignore
                    }
                }

                count--;
            }

            if (mSocketReceiver == null || !mSocketReceiver.isBound()) {
                // something is wrong...
                Log.e(TAG, "receiver socket could not be established");
                return;
            }

            try {
                while (true) {
                    byte[] buf = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    mSocketReceiver.receive(packet);

                    if (mListener != null) {
                        String string = new String(packet.getData());
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "packet received: " + string);
                        } else {
                            Log.v(TAG, "packet received");
                        }

                        try {
                            JSONObject jsonObject = new JSONObject(string);
                            float seconds = (float) jsonObject
                                    .getDouble(Configuration.DATA_KEY_SECONDS);
                            String name = jsonObject
                                    .getString(Configuration.DATA_KEY_NAME);
                            mListener.onBroadcastMessage(seconds, name);
                        } catch (JSONException e) {
                            Log.e(TAG, "receiver socket json", e);
                        }
                    }
                }
            } catch (SocketException e) {
                // ignore
            } catch (Exception e) {
                Log.e(TAG, "receiver socket", e);
            }

            try {
                mSocketReceiver.close();
            } catch (Exception e) {
                Log.e(TAG, "receiver socket closing", e);
            }

            Log.i(TAG, "receiver socket closed");
        }
    }
}
