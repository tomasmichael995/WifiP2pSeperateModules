package gr.kalymnos.skemelio.wifip2pseperatemodulestest.server;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import gr.kalymnos.skemelio.wifip2pseperatemodulestest.MainActivity;

public class Server extends Thread {
    private static final String TAG = MainActivity.TAG + " Server";
    public static final int LOCAL_PORT = 8885;
    private static final int MESSAGE = 1453;

    private ServerSocket serverSocket;
    private List<ServerThread> threads = new ArrayList<>();
    private OnServerConnectionListener callback;

    private boolean started = false;

    public interface OnServerConnectionListener {
        void onClientConnected();

        void onMessageReceived(int message);
    }

    public Server() {
        initializeServerSocket();
    }

    private void initializeServerSocket() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(LOCAL_PORT));
        } catch (IOException e) {
            Log.e(TAG, "Error creating ServerSocket", e);
        }
    }

    @Override
    public void run() {
        started = true;
        while (!Thread.currentThread().isInterrupted()) {
            ServerThread serverThread = new ServerThread(getAcceptedSocket());
            threads.add(serverThread);
            serverThread.start();
            Log.d(TAG, "A client was accepted");
            callback.onClientConnected();
        }
        releaseResources();
    }

    private Socket getAcceptedSocket() {
        try {
            return serverSocket.accept();
        } catch (IOException e) {
            Log.e(TAG, "Error accepting socket.", e);
            return null;
        }
    }

    private void releaseResources() {
        Log.d(TAG, "Server.releaseResources() called");
        closeServerSocket();
        closeServerThreads();
        callback = null;
    }

    private void closeServerSocket() {
        try {
            serverSocket.close();
            Log.d(TAG, "Server socket closed.");
        } catch (IOException e) {
            Log.e(TAG, "Error closing server socket.", e);
        }
    }

    private void closeServerThreads() {
        for (ServerThread thread : threads)
            thread.interrupt();
        threads.clear();
    }

    public void sendMessageToAll() {
        for (ServerThread thread : threads)
            thread.sendMessage();
    }

    public void setOnServerConnectionListener(OnServerConnectionListener listener) {
        callback = listener;
    }

    public boolean hasStarted() {
        return started;
    }

    private class ServerThread extends Thread {
        private static final String TAG = MainActivity.TAG + " ServerThread";

        private Socket socket;
        private InputStream in;
        private OutputStream out;

        ServerThread(@NonNull Socket socket) {
            initializeFields(socket);
        }

        private void initializeFields(@NonNull Socket socket) {
            this.socket = socket;
            initializeInputStream(socket);
            initializeOutputStream(socket);
        }

        private void initializeInputStream(@NonNull Socket socket) {
            try {
                in = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error creating input stream.", e);
            }
        }

        private void initializeOutputStream(@NonNull Socket socket) {
            try {
                out = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error creating output stream.", e);
            }
        }

        @Override
        public void run() {
            readResponsesUntilInterrupted();
            closeSocket();
        }

        private void readResponsesUntilInterrupted() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    int response = in.read();
                    Log.d(TAG, "Read client response: " + response);
                    callback.onMessageReceived(response);
                } catch (IOException e) {
                    Log.e(TAG, "Error while reading or writing response", e);
                }
            }
        }

        private void closeSocket() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error while closing socket.", e);
            }
        }

        void sendMessage() {
            new Thread(() -> {
                try {
                    out.write(MESSAGE);
                } catch (IOException e) {
                    Log.e(TAG, "Error sending message.", e);
                }
            }).start();
        }
    }
}
