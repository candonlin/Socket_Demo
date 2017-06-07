package Client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * Created by can on 2017/6/6.
 */

public class SocketClient {
    private Socket socket;
    private OutputStream socketOutput;


    private String ip;
    private int port;
    private ClientCallback listener = null;

    public SocketClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public boolean isConnect() {
        if (socket != null) {
            socket.isConnected();
        }
        return false;
    }

    public void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                socket = new Socket();
                InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
                try {
                    socket.connect(socketAddress);
                    socketOutput = socket.getOutputStream();


                    // new ReceiveThread().start();
                    Recive();
                    if (listener != null)
                        listener.onConnect(socket);
                } catch (IOException e) {
                    if (listener != null)
                        listener.onConnectError(socket, e.getMessage());
                }
            }
        }).start();
    }

    public void disconnect() {
        try {

            socket.close();
        } catch (IOException e) {
            if (listener != null)
                listener.onDisconnect(socket, e.getMessage());
        }
    }

    public void send(String message) {
        byte[] jsonByte = message.getBytes();

        byte[] intbyte = intToByteArray(jsonByte.length);

        try {
            socketOutput.write(intbyte);
            socketOutput.write(jsonByte);
        } catch (IOException e) {
            if (listener != null)
                listener.onDisconnect(socket, e.getMessage());
        }
    }

    public static byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    public void Recive() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    while (socket.isConnected()) {   // each line must end with a \n to be received

                        byte[] buffer = new byte[4];
                        int readCount1 = 0; // 已经成功读取的字节的个数
                        if (socket.getInputStream().available() > 1) {
                            while (readCount1 < 4 && socket.getInputStream().available() > 1) {
                                readCount1 += socket.getInputStream().read(buffer, readCount1, 4 - readCount1);

                            }
                            if (readCount1 == 4) {

                                int count = IntLength(buffer);
                                byte[] b = new byte[count];
                                int readCount = 0; // 已经成功读取的字节的个数
                                while (readCount < count) {
                                    readCount += socket.getInputStream().read(b, readCount, count - readCount);
                                }

                                if (readCount == count) {
                                    if (listener != null) {
                                        listener.onMessage(new String(b, Charset.forName("UTF-8")));
                                    }

                                }
                            }
                        } else {
                            Thread.sleep(1500);
                        }
                    }

                    if (!socket.isConnected()) {
                        if (listener != null) {
                            listener.onConnectError(socket, "Disconnect");
                        }
                    }

                } catch (IOException e) {
                    if (listener != null) {
                        listener.onConnectError(socket, e.getMessage());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public int IntLength(byte[] buf) {
        return java.nio.ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    public void setClientCallback(ClientCallback listener) {
        this.listener = listener;
    }

    public void removeClientCallback() {
        this.listener = null;
    }

    public interface ClientCallback {
        void onMessage(String message);

        void onConnect(Socket socket);

        void onDisconnect(Socket socket, String message);

        void onConnectError(Socket socket, String message);
    }
}
