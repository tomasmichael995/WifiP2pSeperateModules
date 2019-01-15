package gr.kalymnos.skemelio.wifip2pseperatemodulestest.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import gr.kalymnos.skemelio.wifip2pseperatemodulestest.MainActivity;
import gr.kalymnos.skemelio.wifip2pseperatemodulestest.R;

public class ServerActivity extends AppCompatActivity implements Server.OnServerConnectionListener {
    private static final String TAG = MainActivity.TAG + " ServerActivity";

    private WifiDirectReceiver receiver;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager.ConnectionInfoListener connectionInfoListener;

    private Server server;

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        initializeFields();
        createGroup();
    }

    private void initializeFields() {
        status = findViewById(R.id.connection_status);
        initializeServer();
        initializeReceiver();
        initializeConnectionInfoListener();
    }

    private void initializeServer() {
        server = new Server();
        server.setOnServerConnectionListener(this);
    }

    private void initializeReceiver() {
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), () -> Log.d(TAG, "Channel disconnected."));
        receiver = new WifiDirectReceiver(manager, channel);
    }

    private void initializeConnectionInfoListener() {
        connectionInfoListener = info -> {
            if (info.groupFormed && info.isGroupOwner) {
                if (!server.hasStarted()) {
                    String msg = "A wifi direct group formed and this device is the group owner";
                    Log.d(TAG, msg);
                    status.setText(msg);
                    server.start();
                }
            }
        };
    }

    private void createGroup() {
        manager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Initiating group creation.");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Group creation failed with reason: " + getReason(reason));
            }

            String getReason(int reason) {
                if (reason == WifiP2pManager.BUSY)
                    return "the operation failed because the framework is busy and unable to service the request";
                if (reason == WifiP2pManager.P2P_UNSUPPORTED)
                    return " the operation failed because p2p is unsupported on the device.";
                return "the operation failed due to an internal error. ";

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, getIntentFilter());
    }

    private IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        return filter;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        server.stopExecution();
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Group removed.");
            }

            @Override
            public void onFailure(int i) {
                Log.d(TAG, "Group could not be removed.");
            }
        });
    }

    public void onMessageClick(View view) {
        server.sendMessageToAll();
    }

    @Override
    public void onClientConnected() {
        String msg = "A client connected.";
        Log.d(TAG, msg);
        runOnUiThread(() -> status.setText(msg));
    }

    @Override
    public void onMessageReceived(int message) {
        String text = "Message received from a client: " + message;
        Log.d(TAG, text);
        runOnUiThread(() -> status.append("\n" + text));
    }

    private class WifiDirectReceiver extends BroadcastReceiver {
        private WifiP2pManager mManager;
        private WifiP2pManager.Channel mChannel;

        public WifiDirectReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel) {
            super();
            this.mManager = manager;
            this.mChannel = channel;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION");
                handleWifiStateChange(intent, action);
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
                NetworkInfo networkInfo = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.isConnected()) {
                    // We are connected with the other device, request connection
                    // info to find group owner IP
                    manager.requestConnectionInfo(channel, connectionInfoListener);
                } else {
                    status.setText("No connection");
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            }
        }

        private void handleWifiStateChange(Intent intent, String action) {
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                if (getWifiState(intent) == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Toast.makeText(ServerActivity.this, "Wifi is enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ServerActivity.this, "Wifi is disabled, bye", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }

        private int getWifiState(Intent intent) {
            return intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        }
    }
}
