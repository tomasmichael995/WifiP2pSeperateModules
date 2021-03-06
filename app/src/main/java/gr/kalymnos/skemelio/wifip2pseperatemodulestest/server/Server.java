package gr.kalymnos.skemelio.wifip2pseperatemodulestest.server;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import gr.kalymnos.skemelio.wifip2pseperatemodulestest.MainActivity;

public class Server extends Thread {
    private static final String TAG = MainActivity.TAG + " Server";
    public static final int LOCAL_PORT = 8888;
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

    public void stopExecution() {
        closeServerSocket();
    }

    private void closeServerSocket() {
        // This triggers a SocketException
        // https://stackoverflow.com/questions/2983835/how-can-i-interrupt-a-serversocket-accept-method
        try {
            serverSocket.close();
            Log.d(TAG, "Server socket closed.");
        } catch (IOException e) {
            Log.e(TAG, "Error closing server socket.", e);
        }
    }

    @Override
    public void run() {
        started = true;
        while (true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
                ServerThread serverThread = new ServerThread(socket);
                serverThread.start();
                threads.add(serverThread);
                callback.onClientConnected();
                Log.d(TAG, "A client was accepted");
            } catch (SocketException e) {
                Log.d(TAG, "Closing socket and exiting loop.");
                break;
            } catch (IOException e) {
                Log.e(TAG, "Error accepting socket.", e);
            }
        }
        releaseResources();
    }

    private void releaseResources() {
        Log.d(TAG, "Server.releaseResources() called");
        closeServerThreads();
        callback = null;
    }

    private void closeServerThreads() {
        for (ServerThread thread : threads)
            thread.stopThread();
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


        public void stopThread() {
            closeSocket();
        }

        private void closeSocket() {
            // This triggers a SocketException
            // https://stackoverflow.com/questions/2983835/how-can-i-interrupt-a-serversocket-accept-method
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error while closing socket.", e);
            }
        }

        @Override
        public void run() {
            readResponsesUntilInterrupted();
        }

        private void readResponsesUntilInterrupted() {
            while (true) {
                try {
                    int response = in.read();
                    if (response == -1) {
                        Log.d(TAG, "Client disconnected.");
                        break;
                    } else {
                        Log.d(TAG, "Read client response: " + response);
                        callback.onMessageReceived(response);
                    }
                } catch (IOException e) {
                    if (socket.isClosed()) {
                        Log.d(TAG, "Terminating ServerThread.");
                        break;
                    } else {
                        Log.e(TAG, "Error when in.read()", e);
                    }
                }
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
