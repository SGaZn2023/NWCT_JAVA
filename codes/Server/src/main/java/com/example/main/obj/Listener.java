package com.example.main.obj;

import com.example.main.util.MyIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Listener {
    private ProxyProtocol pProtocol;
    private SessionManager sessionManager;
    private ServerSocket ss;
    private static final byte CMD_END = 0x08;
    private static final String endMessage = "#EELSEYLJTHECONNECTEDISEND#";
    private ThreadPoolExecutor pool = new ThreadPoolExecutor(
            3,  // 核心线程数
            16, // 线程池总大小
            60, // 空闲时间
            TimeUnit.SECONDS,   // 空闲时间单位
            new ArrayBlockingQueue<>(2),    // 队列
            Executors.defaultThreadFactory(),   // 线程工厂，让线程池如何创建对象
            new ThreadPoolExecutor.AbortPolicy()    // 阻塞队列
    );

    public Listener(ProxyProtocol pProtocol, SessionManager sessionManager) throws IOException {
        this.pProtocol = pProtocol;
        this.sessionManager = sessionManager;
    }

    public void listenAndServer() throws IOException {
        switch (pProtocol.publicProtocol) {
            case "tcp":
                this.listenAndServerTCP();
            default:
                throw new IOException("Protocol not supported");
        }
    }

    public void listenAndServerTCP() {
        try {
            ss = new ServerSocket(pProtocol.publicPort);
            Session session = this.sessionManager.getSessionByClientId(pProtocol.clientId);
            byte[] ppBytes = pProtocol.encode();
            System.out.println("ppBytes[1] = " + ppBytes[1]);
            session.send(ppBytes);
            System.out.println("服务器启动成功，等待外界访问");
            // 等待外界访问
            /*while (true) {
                System.out.println("服务器启动，等待外界访问");
                Socket socket = ss.accept();
                System.out.println("外界访问");
                Session session = sessionManager.getSessionByClientId(pProtocol.clientId);
                byte[] pProtocolBytes = this.pProtocol.encode();
                session.send(pProtocolBytes);
                System.out.println("--开始穿透--");
                // 发送数据
                pool.submit(() -> {
                    try {
                        byte[] buffer = new byte[1024];

                        int bytesRead;
                        InputStream inputStream = socket.getInputStream();
                        OutputStream outputStream = session.getOutputStream();
                        while((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            outputStream.flush();
                        }
                        System.out.println("end");
                        // 发送结束标志
                        byte[] end = new byte[1024];
                        end[0] = 0x0;
                        end[1] = CMD_END;
                        byte[] messageBytes = endMessage.getBytes();
                        ByteBuffer byteBuffer = ByteBuffer.wrap(end);
                        byteBuffer.order(ByteOrder.BIG_ENDIAN);
                        byteBuffer.putShort(2, (short) messageBytes.length);

                        for (int i = 0; i < messageBytes.length; i++) {
                            end[i + 4] = messageBytes[i];
                        }

                        outputStream.write(end);
                        System.out.println("send");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                //接收数据
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    OutputStream outputStream = socket.getOutputStream();
                    InputStream inputStream = session.getInputStream();
                    while((bytesRead = inputStream.read(buffer)) != -1) {
                        // 检测结束标志
                        if (bytesRead == 1024 && buffer[1] == CMD_END) {
                            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 2, 2);
                            byteBuffer.order(ByteOrder.BIG_ENDIAN);
                            short length = byteBuffer.getShort();
                            byte[] endMessageBytes = endMessage.getBytes();
                            if (length == endMessageBytes.length) {
                                byte[] message = new byte[length];
                                for (int i = 0; i < endMessageBytes.length; i++)
                                    message[i] = buffer[i + 4];
                                String messageStr = new String(message);
                                if (endMessage.equals(messageStr)) {
                                    break;
                                }
                            }
                        }
                        // 发送
                        outputStream.write(buffer, 0, bytesRead);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                System.out.println("--穿透结束--");
            }*/
            while(true) {
                Socket socket = ss.accept();
                System.out.println("外界访问");
                pool.submit(() -> {
                    handleTcp(session.getSocket(), socket);
                    try {
                        socket.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleTcp(Socket sessionSocket, Socket remoteSocket) {
        try {
            InputStream sessionIn = sessionSocket.getInputStream();
            OutputStream sessionOut = sessionSocket.getOutputStream();
            InputStream remoteIn = remoteSocket.getInputStream();
            OutputStream remoteOut = remoteSocket.getOutputStream();

            System.out.println("--开始穿透--");
            // 向内网发送数据
            pool.submit(() -> {
                try {
                    MyIO.copy(remoteIn, sessionOut);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            // 接收内网发送的数据
            MyIO.copyWithListenEnd(sessionIn, remoteOut);
            System.out.println("--结束穿透--");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
