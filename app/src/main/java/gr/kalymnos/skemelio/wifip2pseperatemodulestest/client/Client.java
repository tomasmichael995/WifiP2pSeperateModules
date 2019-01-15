package gr.kalymnos.skemelio.wifip2pseperatemodulestest.client;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import gr.kalymnos.skemelio.wifip2pseperatemodulestest.MainActivity;
import gr.kalymnos.skemelio.wifip2pseperatemodulestest.server.Server;

public class Client extends Thread {
    private static final String TAG = MainActivity.TAG + " Client";
    private static final int TIMEOUT_MILLI = 500;
    private static final int MESSAGE = 1821;

    private Socket socket;
    private InetAddress groupOwnerAddress;
    private InputStream in;
    private OutputStream out;

    private OnClientConnectionListener callback;

    public interface OnClientConnectionListener {
        void onClientConnected();

        void onResponseRead(int response);

        void onClientConnectionError(String errorLog);
    }

    public Client(InetAddress groupOwnerAddress) {
        initializeFields(groupOwnerAddress);
    }

    private void initializeFields(InetAddress groupOwnerAddress) {
        this.groupOwnerAddress = groupOwnerAddress;
        initializeSocket();
    }

    private void initializeSocket() {
        try {
            socket = new Socket();
            socket.bind(null);
        } catch (IOException e) {
            Log.e(TAG, "Error binding socket.", e);
        }
    }

    @Override
    public void run() {
        connectSocket();
        initializeStreams();
        readUntilInterrupted();
        tearDown();
    }

    private void connectSocket() {
        try {
            socket.connect((new InetSocketAddress(groupOwnerAddress, Server.LOCAL_PORT)), TIMEOUT_MILLI);
            callback.onClientConnected();
        } catch (IOException e) {
            Log.e(TAG, "Error connecting socket to group owner.", e);
            callback.onClientConnectionError(e.getMessage());
        }
    }

    private void initializeStreams() {
        initializeInputStream();
        initializeOutputStream();
    }

    private void initializeInputStream() {
        try {
            in = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error creating input stream from socket.", e);
        }
    }

    private void initializeOutputStream() {
        try {
            out = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error creating output stream from socket.", e);
        }
    }

    private void readUntilInterrupted() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int response = in.read();
                callback.onResponseRead(response);
            } catch (IOException e) {
                Log.e(TAG, "Error reading server response.", e);
            }
        }
    }

    private void tearDown() {
        Log.d(TAG, "Client.tearDown() called.");
        closeSocket();
        callback = null;
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket.", e);
        }
    }

    public void sendMessage() {
        new Thread(() -> {
            try {
                out.write(MESSAGE);
            } catch (IOException e) {
                Log.e(TAG, "Error sending message.", e);
            }
        }).start();
    }

    public void setOnClientConnectionListener(@NonNull OnClientConnectionListener listener) {
        callback = listener;
    }
}
