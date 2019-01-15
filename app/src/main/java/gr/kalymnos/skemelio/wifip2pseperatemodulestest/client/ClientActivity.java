package gr.kalymnos.skemelio.wifip2pseperatemodulestest.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import gr.kalymnos.skemelio.wifip2pseperatemodulestest.MainActivity;
import gr.kalymnos.skemelio.wifip2pseperatemodulestest.R;

public class ClientActivity extends AppCompatActivity implements Client.OnClientConnectionListener {
    private static final String TAG = MainActivity.TAG + " ClientActivity";
    private WifiDirectReceiver receiver;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    private WifiP2pManager.ConnectionInfoListener connectionInfoListener;
    private WifiP2pManager.PeerListListener peerListListener;
    private List<WifiP2pDevice> peers = new ArrayList<>();
    private List<String> peerNames = new ArrayList<>();

    private TextView status;
    private ListView listView;
    private ArrayAdapter<String> listAdapter;

    private Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        initializeFields();
    }

    private void initializeFields() {
        status = findViewById(R.id.connection_status);
        initializePeerListener();
        initializeListView();
        initializeReceiver();
        initializeConnectionInfoListener();
    }

    private void initializePeerListener() {
        peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {
                if (peerList.getDeviceList().size() == 0) {
                    Log.d(TAG, "No devices found");
                } else {
                    repopulatePeers(peerList);
                    repopulatePeerNames();
                    listAdapter.notifyDataSetChanged();
                }

            }

            private void repopulatePeers(WifiP2pDeviceList peerList) {
                peers.clear();
                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    peers.add(device);
                }
            }

            private void repopulatePeerNames() {
                peerNames.clear();
                for (WifiP2pDevice device : peers) {
                    peerNames.add(device.deviceName);
                }
            }

        };
    }

    private void initializeListView() {
        listView = findViewById(R.id.listview);
        listAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, peerNames);
        listView.setAdapter(listAdapter);
        setListviewItemClickListener();
    }

    private void setListviewItemClickListener() {
        listView.setOnItemClickListener((adapterView, view, position, id) -> {
            manager.connect(channel, getWifiP2pConfig(position), new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // WiFiDirectReceiver notifies us. Ignore for now.
                    Log.d(TAG, "Connection initiation successful.");
                }

                @Override
                public void onFailure(int i) {
                    Log.d(TAG, "Connection initiation failed, retry.");
                }
            });
        });
    }

    @NonNull
    private WifiP2pConfig getWifiP2pConfig(int position) {
        WifiP2pDevice device = peers.get(position);
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        return config;
    }

    private void initializeReceiver() {
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WifiDirectReceiver(manager, channel);
    }

    private void initializeConnectionInfoListener() {
        connectionInfoListener = info -> {
            if (info.groupFormed && !info.isGroupOwner) {
                if (client != null) {
                    return;
                } else {
                    initializeClient(info);
                    client.start();
                }
            }
        };
    }

    private void initializeClient(WifiP2pInfo info) {
        client = new Client(info.groupOwnerAddress);
        client.setOnClientConnectionListener(this);
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
        stopClient();
    }

    private void stopClient() {
        if (client != null)
            client.stopExecution();
        client = null;
    }

    public void onDiscoverPeersClick(View view) {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Discovering peers initiation success.");
            }

            @Override
            public void onFailure(int i) {
                Log.d(TAG, "Discovering peers initiation failed.");
            }
        });
    }

    public void onMessageClick(View view) {
        client.sendMessage();
    }

    @Override
    public void onClientConnected() {
        String text = "Connected to group";
        Log.d(TAG, text);
        runOnUiThread(() -> status.setText(text));
    }

    @Override
    public void onResponseRead(int response) {
        Log.d(TAG, "Received response from server: " + response + ".");
        runOnUiThread(() -> status.append("\n Received response " + response));
    }

    @Override
    public void onClientConnectionError(String errorLog) {
        Log.d(TAG, "Client connection error: " + errorLog);
        runOnUiThread(() -> status.setText("Connection error:\n" + errorLog));
        finish();
    }

    private class WifiDirectReceiver extends BroadcastReceiver {
        private WifiP2pManager mManager;
        private WifiP2pManager.Channel mChannel;

        WifiDirectReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel) {
            super();
            mManager = manager;
            mChannel = channel;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION");
                handleWifiStateChange(context, intent);
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
                mManager.requestPeers(mChannel, peerListListener);
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
                NetworkInfo networkInfo = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {
                    // We are connected with the other device, request connection
                    // info to find group owner IP
                    manager.requestConnectionInfo(channel, connectionInfoListener);
                } else {
                    manager.discoverPeers(channel, null);
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
                // Respond to this device's wifi state changing
            }

        }

        private void handleWifiStateChange(Context context, Intent intent) {
            if (getWifiState(intent) == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Toast.makeText(context, "Wifi is enabled.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Not availabie, bye!", Toast.LENGTH_LONG).show();
                finish();
            }
        }

        private int getWifiState(Intent intent) {
            return intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        }
    }
}
