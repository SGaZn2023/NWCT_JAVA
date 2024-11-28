package com.example.main.obj;

import com.example.main.util.MyIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Listener {
    private ProxyProtocol pProtocol;
    private SessionManager sessionManager;
    private ServerSocket ss;
    private MyEncrypt myEncrypt;
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

    public Listener(ProxyProtocol pProtocol, SessionManager sessionManager, MyEncrypt myEncrypt) throws IOException {
        this.pProtocol = pProtocol;
        this.sessionManager = sessionManager;
        this.myEncrypt = myEncrypt;
    }

    public void listenAndServer() throws IOException {
        switch (pProtocol.publicProtocol) {
            case "tcp":
                this.listenAndServerTCP();
            default:
                throw new IOException("暂不支持该协议");
        }
    }

    public void listenAndServerTCP() {
        try {
            ss = new ServerSocket(pProtocol.publicPort);
            // 等待外界访问
            while(true) {
                System.out.println("服务器启动成功，等待外界访问");
                Socket socket = ss.accept();
                System.out.println("外界访问" + (this.myEncrypt.getIsEncrypt() ? "，传输数据已加密" : ""));
                Session session;
                try {
                    session = this.sessionManager.getSessionByClientId(pProtocol.clientId);
                } catch (Exception e) {
                    socket.close();
//                    e.printStackTrace();
                    System.out.println("没有找到相应客户端连接: " + pProtocol.clientId);
                    continue;
                }
//                System.out.println("getSession");
                Socket sessionSocket;
                try {
                    sessionSocket = session.getSocket(pProtocol);
                } catch (Exception e) {
                    socket.close();
//                    e.printStackTrace();
                    System.out.println("与客户端建立连接失败");
                    continue;
                }
//                System.out.println("getSocket");
                new Thread(() -> {
                    if (this.myEncrypt.getIsEncrypt()) {
                        handleTcpWithEncrypt(sessionSocket, socket);
                    } else {
                        handleTcp(sessionSocket, socket);
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
//                        e.printStackTrace();
                        System.out.println("外界 socket 关闭失败");
                    }
                    try {
                        sessionSocket.close();
                    } catch (Exception e) {
//                        e.printStackTrace();
                        System.out.println("客户端 socket 关闭失败");
                    }
                }).start();
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

//            System.out.println("--开始穿透--");
            // 向内网发送数据
            new Thread(() -> {
                try {
                    MyIO.copy(remoteIn, sessionOut);
                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                    e.printStackTrace();
                }
            }).start();
            // 接收内网发送的数据
//            MyIO.copyWithListenEnd(sessionIn, remoteOut);
            MyIO.copy(sessionIn, remoteOut);
//            System.out.println("--结束穿透--");
        } catch (Exception e) {
//            throw new RuntimeException(e);
//            e.printStackTrace();
        }
    }

    public synchronized void handleTcpWithEncrypt(Socket sessionSocket, Socket remoteSocket) {
        if (!this.myEncrypt.getIsEncrypt()) throw new RuntimeException("handleTcp 选择错误");
        byte[] key = myEncrypt.getSecretKey();
        byte[] iv = myEncrypt.getIV();
        try {
            sessionSocket.setSoTimeout(5000);
            remoteSocket.setSoTimeout(5000);
            InputStream sessionIn = sessionSocket.getInputStream();
            OutputStream sessionOut = sessionSocket.getOutputStream();
            InputStream remoteIn = remoteSocket.getInputStream();
            OutputStream remoteOut = remoteSocket.getOutputStream();

//            System.out.println("--开始穿透--");
            // 向内网发送数据
            new Thread(() -> {
                try {
//                    MyIO.copy(remoteIn, sessionOut);
                    MyIO.copyWithEncrypt(remoteIn, sessionOut, key, iv);
                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                    e.printStackTrace();
                }
            }).start();
            // 接收内网发送的数据
//            MyIO.copyWithListenEnd(sessionIn, remoteOut);
//            MyIO.copy(sessionIn, remoteOut);
            MyIO.copyWithDecrypt(sessionIn, remoteOut, key);
//            System.out.println("--结束穿透--");
        } catch (Exception e) {
//            throw new RuntimeException(e);
//            e.printStackTrace();
        }
    }
}
