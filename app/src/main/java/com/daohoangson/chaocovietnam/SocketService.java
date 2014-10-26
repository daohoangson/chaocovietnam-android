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
                e.printStackTrace();

                if (mSocketSender != null) {
                    mSocketSender.close();
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
                mSocketSender.close();
            } catch (Exception e) {
                // this is expected
            }

            Log.i(TAG, "sender socket closed");
        }
    }

    private class ReceiverRunnable implements Runnable {
        private DatagramSocket mSocketReceiver;
        private SocketServiceListener mListener = null;
        private boolean mFlagStop = false;

        private void scheduleStop() {
            mFlagStop = true;
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
                    e.printStackTrace();

                    if (mSocketReceiver != null) {
                        mSocketReceiver.close();
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
                while (!mFlagStop) {
                    byte[] buf = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    mSocketReceiver.receive(packet);

                    if (mListener != null) {
                        String string = new String(packet.getData());

                        Log.v(TAG, "packet received: " + string);

                        JSONObject jsonObject = new JSONObject(string);
                        float seconds = (float) jsonObject
                                .getDouble(Configuration.DATA_KEY_SECONDS);
                        String name = jsonObject
                                .getString(Configuration.DATA_KEY_NAME);
                        mListener.onBroadcastMessage(seconds, name);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                mSocketReceiver.close();
            } catch (Exception e) {
                // this is expected
            }

            Log.i(TAG, "receiver socket closed");
        }
    }
}
