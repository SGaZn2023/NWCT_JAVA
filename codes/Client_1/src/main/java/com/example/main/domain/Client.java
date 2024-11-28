package com.example.main.domain;

import com.example.main.obj.MyEncrypt;
import com.example.main.obj.ProxyProtocol;
//import com.example.main.util.HeartBeat;
import com.example.main.obj.Session;
import com.example.main.util.MyIO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Client {
    private static int sleepTime = 3000;
    private String clientId;    // 不得长于 36 字节
    private String publicIp;    // 不带端口
    private int connectPort;
    private static final byte CMD_END = 0x08;
    private static final String endMessage = "#EELSEYLJTHECONNECTEDISEND#";
    private static final byte CMD_HEARTBEAT = 0x02;
    private static final String heartbeatMessage = "#EELSEYLJTHECONNECTEDISHEARTBEAT#"; // 33字符
//    private boolean isEncrypt;
//    private byte[] secretKey;
//    private byte[] IV;

    private MyEncrypt myEncrypt;

    public static int getSleepTime() {
        return sleepTime;
    }

    public static ThreadPoolExecutor pool = new ThreadPoolExecutor(
            3,  // 核心线程数
            16, // 线程池总大小
            60, // 空闲时间
            TimeUnit.SECONDS,   // 空闲时间单位
            new ArrayBlockingQueue<>(2),    // 队列
            Executors.defaultThreadFactory(),   // 线程工厂，让线程池如何创建对象
            new ThreadPoolExecutor.AbortPolicy()    // 阻塞队列
    );

    private static final byte CMD_HANDSHAKE = 0x01;

    public Client(String clientId, String publicIp, int connectPort) {
        this(clientId, publicIp, connectPort, new MyEncrypt());
    }

    public Client(String clientId, String publicIp, int connectPort, MyEncrypt myEncrypt) {
        this.clientId = clientId;
        this.publicIp = publicIp;
        this.connectPort = connectPort;
//        this.isEncrypt = isEncrypt;
//        if (isEncrypt) {
//            this.secretKey = AESUtil.hexToBytes(secretKey);
//            this.IV = AESUtil.hexToBytes(IV);
//        }
        this.myEncrypt = myEncrypt;
    }

    public void run() throws InterruptedException {
        Session session = new Session();
        new Thread(() -> {
            while(true) {
                try {
                    // 创建三个 socket 一个发送信息，一个发送数据，一个进行心跳检测
                    // 创建messageSocket
                    session.setMessageSocket(new Socket(publicIp, connectPort));
                    Socket socket = session.getMessageSocket();
                    socket.setKeepAlive(true);
                    socket.setSoTimeout(3000);
                    byte[] clientIdBytes = encodeClientId();
                    socket.getOutputStream().write(clientIdBytes);
                    socket.getOutputStream().flush();
                    socket.setSoTimeout(0);
                    // 创建heartbeatSocket
                    session.setHeartbeatSocket(new Socket(publicIp, connectPort));
                    Socket heartbeatSocket = session.getHeartbeatSocket();
                    heartbeatSocket.setKeepAlive(true);
                    heartbeatSocket.setSoTimeout(3000);
                    byte[] heartbeatBytes = encodeHeartbeat();
                    heartbeatSocket.getOutputStream().write(heartbeatBytes);
                    heartbeatSocket.getOutputStream().flush();
                    heartbeatSocket.setSoTimeout(0);
                    System.out.println("服务器成功连接 " + publicIp + ":" + connectPort);

                    while(true) {
                        try {
                            Thread.sleep(5000);
                            heartbeatSocket.getOutputStream().write(heartbeatBytes);
//                            socket.sendUrgentData(0);
                        } catch (Exception e) {
                            System.out.println(publicIp + ":" + connectPort +" 连接错误: " + e.getMessage());
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println(publicIp + ":" + connectPort +" 连接错误: " + e.getMessage());
                    try {
                        Thread.sleep(sleepTime);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                System.out.println("正在重新连接 " + this.publicIp + ":" + this.connectPort);
            }
        }).start();
        while(true) {
            try {
                this.Run(session);
            } catch (Exception e) {
//                System.out.println("穿透出现问题");
                Thread.sleep(sleepTime);
            }
        }
    }

    public void Run(Session session) throws Exception {
        Socket socket = session.getMessageSocket();
        if (socket == null) return;
        // 内网穿透

        InputStream inputStream = socket.getInputStream();
        while (true) {
            ProxyProtocol pProtocol = ProxyProtocol.decode(inputStream);
//            System.out.println("getProxyProtocol");
            if (pProtocol == null) continue;
            Socket localSocket = null;
            switch (pProtocol.internalProtocol) {
                case "tcp":
                    while (true) {
                        try {
                            localSocket = new Socket(pProtocol.internalIp, pProtocol.internalPort);
                            break;
                        } catch (Exception e) {
                            System.out.println("连接本地服务失败");
                            Thread.sleep(sleepTime);
                            System.out.println("正在尝试重新连接");
                        }
                    }
                    Socket serverSocket;
                    while (true) {
                        try {
//                            System.out.println("connect server");
                            serverSocket = createSocketWithServer();
                            break;
                        } catch (Exception e) {
                            Thread.sleep(sleepTime);
                        }
                    }
                    Socket finalLocalSocket = localSocket;
                    Socket finalServerSocket = serverSocket;
                    new Thread(() -> {
                        if (myEncrypt.getIsEncrypt()) {
                            handleTcpWithEncrypt(finalLocalSocket, finalServerSocket);
                        } else {
                            handleTcp(finalLocalSocket, finalServerSocket);
                        }
                        try {
                            finalLocalSocket.close();
                        } catch (IOException e) {
                            System.out.println("与本地服务的连接关闭失败");
                        }
                        try {
                            finalServerSocket.close();
                        } catch (Exception e) {
//                            e.printStackTrace();
                            System.out.println("与服务器的连接关闭失败");
                        }
                    }).start();
                    break;
                default:
                    System.out.println("暂不支持 " + pProtocol.internalProtocol + " 协议");
            }
//            while (true) {
//                try {
//                    localSocket.sendUrgentData(0);
//                } catch (Exception e) {
//                    break;
//                }
//            }
        }
    }

    public Socket createSocketWithServer() throws Exception {
        Socket socket = new Socket(publicIp, connectPort);
        byte[] messages = encodeClientId();
        socket.setSoTimeout(3000);
        socket.getOutputStream().write(messages);
        socket.getOutputStream().flush();
        socket.setSoTimeout(0);
        return socket;
    }

    public void handleTcp(Socket localSocket, Socket remoteSocket) {
        try {
            InputStream localIn = localSocket.getInputStream();
            OutputStream localOut = localSocket.getOutputStream();
            InputStream remoteIn = remoteSocket.getInputStream();
            OutputStream remoteOut = remoteSocket.getOutputStream();

            // 接收服务器信息
            new Thread(() -> {
                try {
//                    MyIO.copyWithListenEnd(remoteIn, localOut);
                    MyIO.copy(remoteIn, localOut);
                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                    System.out.println("数据交换");
                }
            }).start();
            // 向服务器发送信息
            MyIO.copy(localIn, remoteOut);
        } catch (Exception e) {
//            throw new RuntimeException(e);
//            e.printStackTrace();
        }
    }

    public synchronized void handleTcpWithEncrypt(Socket localSocket, Socket remoteSocket) {
        try {
            localSocket.setSoTimeout(5000);
            remoteSocket.setSoTimeout(5000);
            InputStream localIn = localSocket.getInputStream();
            OutputStream localOut = localSocket.getOutputStream();
            InputStream remoteIn = remoteSocket.getInputStream();
            OutputStream remoteOut = remoteSocket.getOutputStream();

            // 接收服务器信息
            new Thread(() -> {
                try {
//                    MyIO.copyWithListenEnd(remoteIn, localOut);
//                    MyIO.copy(remoteIn, localOut);
                    MyIO.copyWithDecrypt(remoteIn, localOut, myEncrypt.getSecretKey());
                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                    System.out.println("数据交换");
                }
            }).start();
            // 向服务器发送信息
//            MyIO.copy(localIn, remoteOut);
            MyIO.copyWithEncrypt(localIn, remoteOut, myEncrypt.getSecretKey(), myEncrypt.getIV());
        } catch (Exception e) {
//            throw new RuntimeException(e);
//            e.printStackTrace();
        }
    }

    public byte[] encodeClientId() throws JsonProcessingException {
        byte[] hdr = new byte[4];
        hdr[0] = 0x0;
        hdr[1] = CMD_HANDSHAKE;

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] body = objectMapper.writeValueAsBytes(clientId);

        ByteBuffer buffer = ByteBuffer.wrap(hdr);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(2, (short) body.length);

        if (hdr.length + body.length > 40) throw new RuntimeException("Client Id too long");
        byte[] result = new byte[40];
        System.arraycopy(hdr, 0, result, 0, hdr.length);
        System.arraycopy(body, 0, result, hdr.length, body.length);

        return result;
    }

    public byte[] encodeHeartbeat() throws JsonProcessingException {
        byte[] hdr = new byte[4];
        hdr[0] = 0x0;
        hdr[1] = CMD_HEARTBEAT;

        byte[] body = heartbeatMessage.getBytes();

        ByteBuffer buffer = ByteBuffer.wrap(hdr);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(2, (short) body.length);

        if (hdr.length + body.length > 40) throw new RuntimeException("33 个字符数错了！！！");
        byte[] result = new byte[40];
        System.arraycopy(hdr, 0, result, 0, hdr.length);
        System.arraycopy(body, 0, result, hdr.length, body.length);

        return result;
    }

}
